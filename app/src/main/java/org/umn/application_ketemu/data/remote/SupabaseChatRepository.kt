package org.umn.application_ketemu.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    private val client: SupabaseClient
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val threads = MutableStateFlow<List<ChatThread>>(emptyList())
    private val messagesByThread = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    /** Kanal Realtime aktif per percakapan (paling banyak satu layar). */
    private val channels = mutableMapOf<String, Pair<RealtimeChannel, Job>>()

    override fun observeThreads(): Flow<List<ChatThread>> =
        threads.asStateFlow().onStart { runCatching { refresh() } }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        kotlinx.coroutines.flow.callbackFlow {
            launch { runCatching { loadMessages(conversationId) } }
            launch { runCatching { startRealtime(conversationId) } }
            messagesByThread
                .onEach { cache -> trySend(cache[conversationId].orEmpty()) }
                .launchIn(this)
            // Layar ditutup → kanal dilepas. Inilah yang membuat pemakaian
            // Realtime hemat: tidak ada langganan yang menggantung.
            awaitClose {
                scope.launch { stopRealtime(conversationId) }
            }
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

    override suspend fun send(conversationId: String, body: String, isTemplate: Boolean): Boolean =
        runCatching {
            val me = client.auth.currentUserOrNull()?.id ?: return false
            val row = client.postgrest
                .from(TABLE_MESSAGES)
                .insert(
                    buildJsonObject {
                        put("conversation_id", conversationId)
                        put("sender_id", me)
                        put("body", body.trim())
                        put("is_template", isTemplate)
                    }
                ) { select(Columns.raw(MESSAGE_COLUMNS)) }
                .decodeSingle<MessageRow>()

            appendMessage(row.toDomain(me))
            touchThread(conversationId, row)
            true
        }.onFailure {
            android.util.Log.e("Chat", "kirim pesan gagal: ${it.message}")
        }.getOrDefault(false)

    override suspend fun refresh() {
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
        val rows = client.postgrest
            .from(TABLE_MESSAGES)
            .select(columns = Columns.raw(MESSAGE_COLUMNS)) {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.ASCENDING)
                limit(MESSAGE_LIMIT.toLong())
            }
            .decodeList<MessageRow>()
        messagesByThread.update { it + (conversationId to rows.map { row -> row.toDomain(me) }) }
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
                        appendMessage(row.toDomain(me))
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
                    lastMessage = row.body,
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
        @SerialName("owner_id") val ownerId: String = "",
        @SerialName("peer_id") val peerId: String = "",
        @SerialName("last_message_at") val lastMessageAt: String? = null,
        val reports: ReportJoin? = null,
        val owner: ProfileJoin? = null,
        val peer: ProfileJoin? = null
    ) {
        /**
         * Baris percakapan → [ChatThread] dari sudut pandang [currentUserId].
         *
         * Lawan bicara ditentukan oleh peran pembaca: pemilik laporan melihat
         * peer, sedangkan peer/petugas melihat pemilik laporan.
         */
        fun toThread(currentUserId: String?): ChatThread? {
            val title = reports?.title ?: return null
            val iAmOwner = currentUserId != null && currentUserId == ownerId
            val partner = if (iAmOwner) peer else owner
            return ChatThread(
                id = id,
                reportId = reportId,
                reportTitle = title,
                reportType = reports?.type.orEmpty(),
                partnerName = partner?.fullName?.ifBlank { null } ?: "Pengguna",
                partnerRole = partner?.role.orEmpty(),
                lastMessage = "",
                lastMessageAt = isoToMillis(lastMessageAt),
                unreadCount = 0
            )
        }
    }

    @Serializable
    internal data class ReportJoin(val title: String = "", val type: String = "")

    @Serializable
    internal data class ProfileJoin(
        @SerialName("full_name") val fullName: String? = null,
        val role: String? = null
    )

    @Serializable
    internal data class MessageRow(
        val id: String = "",
        @SerialName("conversation_id") val conversationId: String = "",
        @SerialName("sender_id") val senderId: String = "",
        val body: String = "",
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("is_template") val isTemplate: Boolean = false
    ) {
        fun toDomain(currentUserId: String?): ChatMessage = ChatMessage(
            id = id,
            threadId = conversationId,
            text = body,
            fromMe = currentUserId != null && senderId == currentUserId,
            sentAt = isoToMillis(createdAt)
        )
    }

    private companion object {
        const val TABLE_CONVERSATIONS = "conversations"
        const val TABLE_MESSAGES = "messages"
        const val SCHEMA = "public"
        const val CHANNEL_PREFIX = "chat-"
        const val THREAD_LIMIT = 50
        const val MESSAGE_LIMIT = 200

        const val THREAD_COLUMNS =
            "id,report_id,owner_id,peer_id,last_message_at," +
                "reports!conversations_report_id_fkey(title,type)," +
                "owner:profiles!conversations_owner_id_fkey(full_name,role)," +
                "peer:profiles!conversations_peer_id_fkey(full_name,role)"

        const val MESSAGE_COLUMNS =
            "id,conversation_id,sender_id,body,created_at,is_template"

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
