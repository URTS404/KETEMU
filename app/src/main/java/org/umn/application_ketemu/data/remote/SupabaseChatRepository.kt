package org.umn.application_ketemu.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.umn.application_ketemu.data.repository.PendingChatStore
import org.umn.application_ketemu.domain.model.MessageStatus
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.ChatThread
import org.umn.application_ketemu.domain.model.TemplateReply
import org.umn.application_ketemu.domain.repository.ChatRepository
import java.time.OffsetDateTime

/**
 * [ChatRepository] di atas tabel `conversations`/`messages` (`db/0019`).
 *
 * Strategi "hemat" yang dipakai:
 *  - **Realtime hanya saat layar percakapan terbuka** — kanal dibuat ketika
 *    `observeMessages()` mulai dikoleksi dan dilepas saat layar ditutup
 *    (`awaitClose`), bukan langganan abadi per pengguna.
 *  - **Tidak ada polling.** Daftar utas disegarkan saat layar dibuka atau saat
 *    pengguna menekan "Muat ulang".
 *  - **Kirim pesan memakai `select()`**: server mengembalikan baris yang baru
 *    dibuat, jadi pesan langsung ditambahkan ke cache tanpa permintaan kedua.
 *  - Proyeksi kolom dibatasi dan daftar utas dibatasi [THREAD_LIMIT].
 */
class SupabaseChatRepository(
    private val client: SupabaseClient,
    private val pendingStore: PendingChatStore,
    private val pendingDir: java.io.File
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val threads = MutableStateFlow<List<ChatThread>>(emptyList())
    private val messagesByThread = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    /** Pesan yang belum terkirim (outbox lokal), dibaca ulang dari disk. */
    private val outbox = MutableStateFlow(pendingStore.load())

    /** Kanal Realtime aktif per percakapan (paling banyak satu layar). */
    private val channels = mutableMapOf<String, Pair<RealtimeChannel, Job>>()

    override fun observeThreads(): Flow<List<ChatThread>> =
        threads.asStateFlow().onStart { runCatching { refresh() } }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        kotlinx.coroutines.flow.callbackFlow {
            launch { runCatching { loadMessages(conversationId) } }
            launch { runCatching { flushPending() } }
            launch { runCatching { startRealtime(conversationId) } }
            kotlinx.coroutines.flow.combine(messagesByThread, outbox) { cache, pending ->
                gabungPesan(cache[conversationId].orEmpty(), pending.filter { it.threadId == conversationId })
            }.collect { trySend(it) }
            // Layar ditutup → kanal dilepas. Inilah yang membuat pemakaian
            // Realtime hemat: tidak ada langganan yang menggantung.
            awaitClose {
                scope.launch { stopRealtime(conversationId) }
            }
        }

    /**
     * Gabungkan pesan server dengan antrean lokal.
     *
     * Dua lapis deduplikasi (inilah yang sebelumnya membuat gelembung berganda):
     *  1. berdasarkan id - satu pesan hanya boleh muncul sekali;
     *  2. pesan antrean yang isinya sudah ada di server dalam 5 menit terakhir
     *     dianggap sudah terkirim, jadi tidak ditampilkan lagi sebagai tertunda.
     */
    /**
     * Membuang entri antrean yang pesannya ternyata SUDAH ada di server.
     *
     * Tanpa ini, entri lama bisa bertahan selamanya di penyimpanan dan terus
     * tampil sebagai "menunggu terkirim" meski pesannya sudah terkirim.
     */
    private fun bersihkanAntreanUsang(terkirim: List<ChatMessage>, threadId: String) {
        val tertunda = pendingStore.load().filter { it.threadId == threadId }
        if (tertunda.isEmpty()) return

        val penandaTerkirim = terkirim
            .map { it.text.trim() to (it.sentAt / WINDOW_DUPLIKAT_MS) }
            .toHashSet()

        tertunda.forEach { pesan ->
            val cocok = pesan.text.trim() to (pesan.sentAt / WINDOW_DUPLIKAT_MS)
            if (cocok in penandaTerkirim) {
                android.util.Log.d("Chat", "antrean usang dibuang: " + pesan.id)
                pendingStore.remove(pesan.id)
            }
        }
        outbox.value = pendingStore.load()
    }

    private fun gabungPesan(
        terkirim: List<ChatMessage>,
        tertunda: List<ChatMessage>
    ): List<ChatMessage> {
        val idTerkirim = terkirim.mapTo(HashSet()) { it.id }
        val penandaTerkirim = terkirim.map { it.text.trim() to (it.sentAt / WINDOW_DUPLIKAT_MS) }.toHashSet()

        val sisa = tertunda.filter { pesan ->
            pesan.id !in idTerkirim &&
                (pesan.text.trim() to (pesan.sentAt / WINDOW_DUPLIKAT_MS)) !in penandaTerkirim
        }
        return (terkirim + sisa).sortedBy { it.sentAt }
    }

    override suspend fun startConversation(reportId: String): String? = runCatching {
        val id = client.postgrest
            .rpc("start_conversation", buildJsonObject { put("p_report_id", reportId) })
            .decodeAs<String>()
        refresh()
        id
    }.onFailure {
        android.util.Log.e("Chat", "start_conversation gagal: ${it.message}")
    }.getOrNull()

    override suspend fun send(conversationId: String, body: String, isTemplate: Boolean): Boolean {
        val teks = body.trim()
        if (teks.isEmpty()) return false
        return try {
            val me = client.auth.currentUserOrNull()?.id ?: return false
            val row = client.postgrest
                .from(TABLE_MESSAGES)
                .insert(
                    buildJsonObject {
                        put("conversation_id", conversationId)
                        put("sender_id", me)
                        put("body", teks)
                        put("is_template", isTemplate)
                    }
                ) { select(Columns.raw(MESSAGE_COLUMNS)) }
                .decodeSingle<MessageRow>()

            appendMessage(row.toDomain(me))
            touchThread(conversationId, row)
            true
        } catch (error: Exception) {
            // Offline / server menolak: pesan TIDAK hilang, tapi ditahan di
            // antrean lokal dan tampil sebagai gelembung "menunggu terkirim".
            android.util.Log.e("Chat", "kirim pesan gagal, masuk antrean: ${'$'}{error.message}")
            enqueue(conversationId, teks)
            false
        }
    }

    /**
     * Mengirim satu entri antrean, baik teks maupun gambar.
     *
     * Entri bergambar harus diunggah dulu ke Storage; sebelumnya jalur ini
     * hanya mengirim teks sehingga gambar tertunda tidak pernah terkirim.
     */
    private suspend fun kirimSatu(pesan: ChatMessage, me: String): MessageRow {
        val lokal = pesan.localImagePath
        if (lokal != null) {
            val berkas = java.io.File(lokal)
            check(berkas.exists()) { "berkas antrean hilang: " + lokal }
            val path = pesan.threadId + "/" + java.util.UUID.randomUUID() + ".webp"
            client.storage.from(BUCKET_IMAGES).upload(path, berkas.readBytes()) { upsert = false }
            return client.postgrest
                .from(TABLE_MESSAGES)
                .insert(
                    buildJsonObject {
                        put("conversation_id", pesan.threadId)
                        put("sender_id", me)
                        put("body", pesan.text.trim().ifEmpty { null })
                        put("image_path", path)
                        put("image_width", pesan.imageWidth)
                        put("image_height", pesan.imageHeight)
                        put("is_template", false)
                    }
                ) { select(Columns.raw(MESSAGE_COLUMNS_GAMBAR)) }
                .decodeSingle<MessageRow>()
        }
        return client.postgrest
            .from(TABLE_MESSAGES)
            .insert(
                buildJsonObject {
                    put("conversation_id", pesan.threadId)
                    put("sender_id", me)
                    put("body", pesan.text)
                    put("is_template", false)
                }
            ) { select(Columns.raw(MESSAGE_COLUMNS_GAMBAR)) }
            .decodeSingle<MessageRow>()
    }

    /** Kirim ulang semua pesan yang tertahan (dipanggil saat jaringan kembali). */
    override suspend fun flushPending() {
        val tertunda = pendingStore.load()
        if (tertunda.isEmpty()) {
            // Penting: state outbox tetap disegarkan walau antrean kosong.
            // Tanpa ini, tampilan bisa memegang salinan lama dan gelembung
            // "menunggu terkirim" tetap muncul padahal sudah terkirim.
            outbox.value = emptyList()
            return
        }
        val me = client.auth.currentUserOrNull()?.id ?: return

        val percakapanBerubah = mutableSetOf<String>()
        tertunda.forEach { pesan ->
            runCatching {
                val row = kirimSatu(pesan, me)

                pendingStore.remove(pesan.id)
                appendMessage(row.toDomain(me))
                touchThread(pesan.threadId, row)
                percakapanBerubah += pesan.threadId
            }.onFailure {
                android.util.Log.e("Chat", "kirim ulang gagal: " + it.message)
            }
        }
        outbox.value = pendingStore.load()
        // Muat ulang daftar pesan supaya gelembung "menunggu terkirim"
        // langsung berganti menjadi pesan (gambar) yang benar-benar terkirim.
        percakapanBerubah.forEach { id ->
            runCatching { loadMessages(id) }
        }
    }

    /** Kirim ulang satu pesan tertentu (tombol putar ulang di samping gelembung). */
    override suspend fun retry(messageId: String): Boolean {
        val pesan = pendingStore.load().firstOrNull { it.id == messageId } ?: return false
        // Cari percakapan asal pesan ini supaya daftarnya bisa dimuat ulang
        // setelah penghapusan (kalau tidak, layar tampak tidak berubah).
        val threadId = messagesByThread.value.entries
            .firstOrNull { (_, daftar) -> daftar.any { it.id == messageId } }
            ?.key

        val me = client.auth.currentUserOrNull()?.id ?: return false
        val berhasil = runCatching {
            val row = kirimSatu(pesan, me)

            pendingStore.remove(pesan.id)
            appendMessage(row.toDomain(me))
            touchThread(pesan.threadId, row)
        }.isSuccess

        outbox.value = pendingStore.load()
        return berhasil
    }

    /**
     * Unggah gambar lalu simpan pesannya.
     *
     * Kalau unggah gagal (offline), gambar disalin dulu ke penyimpanan aplikasi
     * dan dimasukkan ke antrean offline — jadi pengguna tetap melihat gelembung
     * buram "menunggu terkirim" dan gambarnya tidak hilang.
     */
    override suspend fun sendImage(
        conversationId: String,
        bytes: ByteArray,
        width: Int,
        height: Int,
        caption: String
    ): Boolean {
        val me = client.auth.currentUserOrNull()?.id ?: return false
        val nama = java.util.UUID.randomUUID().toString()
        val path = "$conversationId/$nama.webp"

        val berhasil = runCatching {
            client.storage.from(BUCKET_IMAGES).upload(path, bytes) { upsert = false }
            val row = client.postgrest
                .from(TABLE_MESSAGES)
                .insert(
                    buildJsonObject {
                        put("conversation_id", conversationId)
                        put("sender_id", me)
                        put("body", caption.trim().ifEmpty { null })
                        put("image_path", path)
                        put("image_width", width)
                        put("image_height", height)
                        put("is_template", false)
                    }
                ) { select(Columns.raw(MESSAGE_COLUMNS_GAMBAR)) }
                .decodeSingle<MessageRow>()

            appendMessage(row.toDomain(me, imageUrl = signedUrl(path)))
            touchThread(conversationId, row)
            // Muat ulang dari server supaya gelembung gambar langsung tampil
            // (sebelumnya baru muncul setelah layar ditutup dan dibuka lagi).
            loadMessages(conversationId)
        }.onFailure {
            android.util.Log.e("Chat", "unggah gambar gagal: " + it.message)
        }.isSuccess

        if (!berhasil) {
            // Simpan salinan lokal supaya gelembung buram tetap bisa ditampilkan.
            val berkas = java.io.File(
                java.io.File(pendingDir, "outbox-" + nama + ".webp").also {
                    it.parentFile?.mkdirs()
                }.path
            )
            runCatching { berkas.writeBytes(bytes) }
            val pesan = ChatMessage(
                id = "local-" + nama,
                threadId = conversationId,
                text = caption,
                fromMe = true,
                sentAt = System.currentTimeMillis(),
                status = MessageStatus.SENDING,
                localImagePath = berkas.path,
                imageWidth = width,
                imageHeight = height
            )
            pendingStore.add(pesan)
            outbox.value = pendingStore.load()
        }
        return berhasil
    }

    /** URL sementara (1 jam) untuk gambar privat di bucket `chat-images`. */
    private suspend fun signedUrl(path: String): String? = runCatching {
        client.storage.from(BUCKET_IMAGES)
            .createSignedUrl(path, 1.hours)
    }.getOrNull()

    /** Hapus pesan sendiri (menu titik-tiga pada gelembung). */
    override suspend fun deleteMessage(messageId: String): Boolean {
        android.util.Log.d("Chat", "deleteMessage mulai: " + messageId)

        // Pesan yang belum terkirim hidup hanya di antrean lokal.
        if (pendingStore.load().any { it.id == messageId }) {
            pendingStore.remove(messageId)
            outbox.value = pendingStore.load()
            android.util.Log.d("Chat", "hapus pesan dari antrean lokal: " + messageId)
            return true
        }

        val threadId = messagesByThread.value.entries
            .firstOrNull { (_, daftar) -> daftar.any { it.id == messageId } }
            ?.key

        val me = client.auth.currentUserOrNull()?.id
        android.util.Log.d("Chat", "deleteMessage: me=" + me)
        if (me == null) return false

        val berhasil = runCatching {
            client.postgrest.from(TABLE_MESSAGES).delete {
                filter {
                    eq("id", messageId)
                    eq("sender_id", me)
                }
            }
        }.onFailure {
            android.util.Log.e("Chat", "hapus pesan gagal: " + it.message)
        }.isSuccess

        if (berhasil) {
            messagesByThread.update { peta ->
                peta.mapValues { (_, daftar) -> daftar.filterNot { it.id == messageId } }
            }
            android.util.Log.i("Chat", "hapus pesan berhasil: " + messageId)
            threadId?.let { id -> runCatching { loadMessages(id) } }
        }
        return berhasil
    }

    private fun enqueue(conversationId: String, body: String) {
        val pesan = ChatMessage(
            id = "local-${'$'}{java.util.UUID.randomUUID()}",
            threadId = conversationId,
            text = body,
            fromMe = true,
            sentAt = System.currentTimeMillis(),
            status = MessageStatus.SENDING
        )
        pendingStore.add(pesan)
        outbox.value = pendingStore.load()
    }

    override suspend fun refresh() {
        runCatching { flushPending() }
        runCatching { threads.value = loadThreads() }
            .onFailure { android.util.Log.e("Chat", "muat utas gagal: ${it.message}") }
    }

    override fun templates(): List<TemplateReply> = TEMPLATES

    // ---------------------------------------------------------------- baca data

    private suspend fun loadThreads(): List<ChatThread> {
        val me = client.auth.currentUserOrNull()?.id
        return client.postgrest
            .from(TABLE_CONVERSATIONS)
            .select(columns = Columns.raw(THREAD_COLUMNS)) {
                order("last_message_at", Order.DESCENDING)
                limit(THREAD_LIMIT.toLong())
            }
            .decodeList<ConversationRow>()
            .mapNotNull { it.toThread(me) }
    }

    private suspend fun loadMessages(conversationId: String) {
        val me = client.auth.currentUserOrNull()?.id

        suspend fun ambil(kolom: String) = client.postgrest
            .from(TABLE_MESSAGES)
            .select(columns = Columns.raw(kolom)) {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.ASCENDING)
                limit(MESSAGE_LIMIT.toLong())
            }
            .decodeList<MessageRow>()
        // Kolom gambar diminta terpisah: bila database belum memilikinya
        // (skrip 0022 belum jalan atau cache skema basi), percakapan TIDAK
        // boleh tampak kosong - kita ulangi dengan kolom dasar dan mencatat galat.
        val rows = runCatching { ambil(MESSAGE_COLUMNS_GAMBAR) }
            .getOrElse { galat ->
                android.util.Log.e("Chat", "gagal muat kolom gambar: " + galat.message)
                ambil(MESSAGE_COLUMNS)
            }

        val pesan = rows.map { row ->
            row.toDomain(me, imageUrl = row.imagePath?.let { path -> signedUrl(path) })
        }

        // Entri antrean yang pesannya sudah ada di server tidak boleh
        // menggantung di tampilan sebagai "menunggu terkirim".
        runCatching { bersihkanAntreanUsang(pesan, conversationId) }
        messagesByThread.update { it + (conversationId to pesan) }
    }

    // -------------------------------------------------------------- realtime

    /**
     * Langganan INSERT pesan untuk satu percakapan. Realtime tetap menghormati
     * RLS, jadi petugas menerima semua pesan dan mahasiswa hanya pesan utasnya.
     */
    private suspend fun startRealtime(conversationId: String) {
        if (channels.containsKey(conversationId)) return
        val me = client.auth.currentUserOrNull()?.id
        val channel = client.realtime.channel("$CHANNEL_PREFIX$conversationId")
        val job = channel
            .postgresChangeFlow<PostgresAction.Insert>(schema = SCHEMA) {
                table = TABLE_MESSAGES
            }
            .onEach { action ->
                runCatching { action.record.toMessageRow() }
                    .getOrNull()
                    ?.takeIf { it.conversationId == conversationId }
                    ?.let { row ->
                        // Gambar privat butuh URL sementara; tanpa ini gelembung
                        // gambar baru terlihat setelah layar dimuat ulang.
                        val url = row.imagePath?.let { path -> signedUrl(path) }
                        appendMessage(row.toDomain(me, imageUrl = url))
                        touchThread(conversationId, row)
                    }
            }
            .launchIn(scope)

        channel.subscribe()
        channels[conversationId] = channel to job
    }

    /** Lepas kanal; dipanggil saat layar percakapan ditutup. */
    suspend fun stopRealtime(conversationId: String) {
        val entry = channels.remove(conversationId) ?: return
        entry.second.cancel()
        runCatching { client.realtime.removeChannel(entry.first) }
    }

    // --------------------------------------------------------------- utilitas

    /** Tambahkan pesan ke cache tanpa duplikasi (Realtime + hasil INSERT bisa sama). */
    private fun appendMessage(message: ChatMessage) {
        messagesByThread.update { current ->
            val list = current[message.threadId].orEmpty()
            if (list.any { it.id == message.id }) current
            else current + (message.threadId to (list + message).sortedBy { it.sentAt })
        }
    }

    /** Perbarui ringkasan utas secara lokal, tanpa query tambahan. */
    private fun touchThread(conversationId: String, row: MessageRow) {
        threads.update { list ->
            list.map { thread ->
                if (thread.id != conversationId) thread
                else thread.copy(
                    lastMessage = row.body.orEmpty().ifBlank { "[gambar]" },
                    lastMessageAt = isoToMillis(row.createdAt)
                )
            }
        }
    }

    // ------------------------------------------------------------------ model

    @Serializable
    internal data class ConversationRow(
        val id: String = "",
        @SerialName("report_id") val reportId: String = "",
        @SerialName("report_title") val reportTitle: String? = null,
        @SerialName("report_type") val reportType: String? = null,
        @SerialName("partner_name") val partnerName: String? = null,
        @SerialName("partner_role") val partnerRole: String? = null,
        @SerialName("last_message") val lastMessage: String? = null,
        @SerialName("last_message_at") val lastMessageAt: String? = null
    ) {
        /** View `chat_threads` sudah memilih lawan bicara sesuai pembaca. */
        fun toThread(@Suppress("UNUSED_PARAMETER") currentUserId: String?): ChatThread? {
            val title = reportTitle ?: return null
            return ChatThread(
                id = id,
                reportId = reportId,
                reportTitle = title,
                reportType = reportType.orEmpty(),
                partnerName = partnerName?.ifBlank { null } ?: "Pengguna",
                partnerRole = partnerRole.orEmpty(),
                lastMessage = lastMessage.orEmpty(),
                lastMessageAt = isoToMillis(lastMessageAt),
                unreadCount = 0
            )
        }
    }

    @Serializable
    internal data class MessageRow(
        val id: String = "",
        @SerialName("conversation_id") val conversationId: String = "",
        @SerialName("sender_id") val senderId: String = "",
        val body: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("is_template") val isTemplate: Boolean = false,
        @SerialName("image_path") val imagePath: String? = null,
        @SerialName("image_width") val imageWidth: Int? = null,
        @SerialName("image_height") val imageHeight: Int? = null
    ) {
        fun toDomain(currentUserId: String?, imageUrl: String? = null): ChatMessage = ChatMessage(
            id = id,
            threadId = conversationId,
            text = body.orEmpty(),
            fromMe = currentUserId != null && senderId == currentUserId,
            sentAt = isoToMillis(createdAt),
            imageUrl = imageUrl,
            imageWidth = imageWidth ?: 0,
            imageHeight = imageHeight ?: 0
        )
    }

    private companion object {
        const val TABLE_CONVERSATIONS = "chat_threads"
        const val BUCKET_IMAGES = "chat-images"
        const val TABLE_MESSAGES = "messages"
        const val SCHEMA = "public"
        const val CHANNEL_PREFIX = "chat-"
        const val THREAD_LIMIT = 50
        const val MESSAGE_LIMIT = 200

        /** Jendela waktu untuk mengenali pesan antrean yang sebenarnya sudah terkirim. */
        const val WINDOW_DUPLIKAT_MS = 5 * 60 * 1000L

        const val THREAD_COLUMNS =
            "id,report_id,report_title,report_type,partner_name,partner_role," +
                "last_message,last_message_at"

        const val MESSAGE_COLUMNS =
            "id,conversation_id,sender_id,body,created_at,is_template"

        /** Kolom tambahan; diminta terpisah supaya kegagalan tidak mengosongkan layar. */
        const val MESSAGE_COLUMNS_GAMBAR =
            MESSAGE_COLUMNS + ",image_path,image_width,image_height"

        /** Balasan cepat petugas (opsi 5C) — teks, tanpa lampiran. */
        val TEMPLATES = listOf(
            TemplateReply("Barang ada di Pos", "Barangnya ada di Pos Basement, silakan datang dengan membawa KTM."),
            TemplateReply("Sedang diperiksa", "Terima kasih informasinya, barang sedang kami periksa di Pos."),
            TemplateReply("Cocokkan ciri", "Boleh sebutkan ciri khusus barangnya supaya kami bisa memastikan?"),
            TemplateReply("Ajukan klaim", "Silakan ajukan klaim di halaman laporan agar bisa kami proses.")
        )


    }
}

/** Decoder longgar untuk payload Realtime (berisi seluruh kolom tabel). */
private fun kotlinx.serialization.json.JsonObject.toMessageRow(): SupabaseChatRepository.MessageRow? =
    runCatching {
        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromJsonElement(SupabaseChatRepository.MessageRow.serializer(), this)
    }.getOrNull()

/** ISO-8601 dari PostgREST → epoch millis; 0 bila kosong/tidak valid. */
private fun isoToMillis(value: String?): Long =
    value?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrDefault(0L) } ?: 0L
