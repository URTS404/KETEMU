package org.umn.application_ketemu.data.repository

import android.util.Log
import androidx.room.withTransaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.local.KetemuDatabase
import org.umn.application_ketemu.data.local.OutboxEntity
import org.umn.application_ketemu.data.local.OutboxKind
import org.umn.application_ketemu.data.local.ReportEntity
import org.umn.application_ketemu.data.local.toDomain
import org.umn.application_ketemu.data.local.toEntity
import org.umn.application_ketemu.data.remote.REPORT_SELECT_COLUMNS
import org.umn.application_ketemu.data.remote.ReportRow
import org.umn.application_ketemu.data.remote.toDomain
import org.umn.application_ketemu.domain.model.HasilCariFoto
import org.umn.application_ketemu.domain.model.PosLocation
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.domain.repository.ReportRepository
import java.util.UUID

/**
 * Implementasi [ReportRepository] untuk tahap M3/M4: **Room sebagai satu-satunya
 * sumber data yang dibaca UI**, Supabase hanya mengisi dan menerima perubahan.
 *
 * Alur tulis (offline-first):
 * 1. Aksi pengguna ditulis ke `reports_cache` dengan status [SyncStatus.PENDING]
 *    **dan** dicatat ke tabel `outbox` dalam SATU transaksi Room.
 * 2. UI langsung melihat perubahan karena membaca Flow dari Room.
 * 3. [dorongAntrean] mengirim isi `outbox` berurutan; baris yang berhasil
 *    dihapus dan laporan ditandai SYNCED.
 * 4. Bila jaringan belum ada, antrean menunggu dan dikirim otomatis begitu
 *    [NetworkMonitor.isOnline] kembali true (atau pada putaran berikutnya).
 *
 * Alur baca: [tarikDariServer] mengambil snapshot `reports` lalu menimpanya ke
 * Room. Baris yang masih PENDING **tidak** ditimpa agar perubahan lokal yang
 * belum terkirim tidak hilang.
 */
class RoomReportRepository(
    private val client: SupabaseClient,
    private val db: KetemuDatabase,
    private val networkMonitor: NetworkMonitor
) : ReportRepository {

    private val laporan = db.reportDao()
    private val antrean = db.outboxDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Mencegah dua sinkronisasi berjalan bersamaan (mis. timer + jaringan pulih). */
    private val kunciSinkron = Mutex()

    init {
        scope.launch {
            while (isActive) {
                sinkronkan()
                delay(REFRESH_INTERVAL_MS)
            }
        }
        // Jaringan kembali -> segera kirim antrean, jangan tunggu timer.
        scope.launch {
            networkMonitor.isOnline.collect { daring -> if (daring) sinkronkan() }
        }
    }

    override fun observeReports(): Flow<List<Report>> =
        aliranLaporan().map { daftar -> daftar.filterNot { it.isArchived } }

    override fun observeArchivedReports(): Flow<List<Report>> =
        aliranLaporan().map { daftar -> daftar.filter { it.isArchived } }

    override fun observeModerationQueue(): Flow<List<Report>> =
        aliranLaporan().map { daftar ->
            daftar
                .filter { !it.isArchived && it.status == ReportStatus.DILAPORKAN }
                .sortedBy { it.createdAt }
        }

    override fun observeMyReports(ownerId: String): Flow<List<Report>> =
        aliranLaporan().map { daftar -> daftar.filter { it.ownerId == ownerId } }

    override suspend fun getReport(id: String): Report? {
        laporan.findById(id)?.let { return it.toDomain() }
        // Belum ada di perangkat (mis. tautan dari notifikasi): coba ambil sekali.
        runCatching { tarikSatu(id) }
            .onFailure { Log.e(TAG, "gagal ambil laporan $id: ${it.message}") }
        return laporan.findById(id)?.toDomain()
    }

    override suspend fun addReport(report: Report) {
        db.withTransaction {
            laporan.upsert(report.toEntity(SyncStatus.PENDING))
            antrean.insert(
                OutboxEntity(
                    kind = OutboxKind.CREATE_REPORT.name,
                    reportId = report.id,
                    payload = json.encodeToString(
                        LaporanPayload.serializer(),
                        LaporanPayload.dari(report)
                    ),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        Log.d(TAG, "addReport: ${report.id} disimpan lokal (PENDING) + antrean")
        sinkronkan()
    }

    override suspend fun updateStatus(reportId: String, status: ReportStatus) {
        ubahLokal(reportId) { it.copy(status = status.name, syncStatus = SyncStatus.PENDING.name) }
        antrekan(
            reportId,
            OutboxKind.UPDATE_STATUS,
            json.encodeToString(StatusPayload.serializer(), StatusPayload(status.name))
        )
        sinkronkan()
    }

    override suspend fun setArchived(reportId: String, archived: Boolean) {
        ubahLokal(reportId) { it.copy(isArchived = archived, syncStatus = SyncStatus.PENDING.name) }
        antrekan(
            reportId,
            OutboxKind.ARCHIVE,
            json.encodeToString(ArsipPayload.serializer(), ArsipPayload(archived))
        )
        sinkronkan()
    }

    override suspend fun confirmHandover(reportId: String, pos: PosLocation) {
        ubahLokal(reportId) {
            it.copy(
                status = ReportStatus.DISERAHKAN_KE_POS.name,
                posId = pos.id,
                posName = pos.name,
                posBuilding = pos.building,
                syncStatus = SyncStatus.PENDING.name
            )
        }
        antrekan(
            reportId,
            OutboxKind.HANDOVER,
            json.encodeToString(
                SerahTerimaPayload.serializer(),
                SerahTerimaPayload(posId = pos.id, posName = pos.name, posBuilding = pos.building)
            )
        )
        sinkronkan()
    }

    /**
     * Menghapus laporan milik pengguna dari server lalu dari cache lokal.
     *
     * Sengaja TIDAK memakai antrean `outbox`: penghapusan harus benar-benar
     * terjadi agar pengguna tidak melihat laporan "hilang" padahal masih ada di
     * server. Karena itu operasi ini memerlukan jaringan; saat offline
     * dilempar [IllegalStateException] agar UI bisa menampilkan pesan jelas
     * (berbeda dengan aksi lain yang tetap boleh diantrekan).
     *
     * Server tetap bisa menolak (klaim masih aktif) — galat dari PostgREST
     * diteruskan apa adanya supaya pesannya bisa diterjemahkan layer UI.
     */
    override suspend fun simpanVektorFoto(reportId: String, vektor: FloatArray): Boolean {
        if (vektor.isEmpty()) return false
        val daring = runCatching { networkMonitor.isOnline.first() }.getOrDefault(false)
        if (!daring) {
            Log.d(TAG, "simpanVektorFoto: offline, sidik foto $reportId ditunda")
            return false
        }

        return runCatching {
            val angka = vektor.map { it.toDouble() }
            client.postgrest.rpc(
                "simpan_vektor_foto",
                buildJsonObject {
                    put("p_report_id", reportId)
                    putJsonArray("p_vektor") { angka.forEach { nilai -> add(JsonPrimitive(nilai)) } }
                },
            ).decodeAs<Boolean>()
        }.onFailure {
            // Fungsi belum dipasang (db/0032 belum dijalankan) bukan alasan
            // menggagalkan simpan laporan; cukup dicatat untuk penelusuran.
            Log.w(TAG, "simpanVektorFoto gagal untuk $reportId: ${it.message}")
        }.getOrDefault(false)
    }

    /**
     * Cari barang temuan yang fotonya mirip dengan [vektor] lewat RPC
     * `cari_barang_mirip` (db/0032). Perhitungan kemiripan terjadi di database
     * memakai indeks HNSW, jadi foto pengguna tidak pernah dikirim ke layanan
     * luar dan tidak ada foto lain yang perlu diunduh ke HP.
     *
     * Kegagalan (offline, fungsi belum dipasang, vektor ditolak) menghasilkan
     * daftar kosong + catatan log: dari sisi pengguna artinya "belum ada yang
     * mirip", bukan galat yang perlu ditampilkan sebagai crash.
     */
    override suspend fun cariDenganFoto(vektor: FloatArray, batas: Int): List<HasilCariFoto> {
        if (vektor.size != DIMENSI_VEKTOR) return emptyList()
        val daring = runCatching { networkMonitor.isOnline.first() }.getOrDefault(false)
        if (!daring) {
            Log.d(TAG, "cariDenganFoto: offline, pencarian foto ditunda")
            return emptyList()
        }

        return runCatching {
            val angka = vektor.map { it.toDouble() }
            client.postgrest.rpc(
                "cari_barang_mirip",
                buildJsonObject {
                    putJsonArray("p_vektor") { angka.forEach { nilai -> add(JsonPrimitive(nilai)) } }
                    put("p_batas", batas)
                },
            ).decodeList<CariFotoRow>().mapNotNull { baris ->
                val status = ReportStatus.entries.firstOrNull { it.name == baris.status }
                if (status == null) {
                    Log.w(TAG, "cariDenganFoto: status '${baris.status}' tidak dikenal, dilewati")
                    null
                } else {
                    HasilCariFoto(
                        id = baris.id,
                        title = baris.title,
                        photoUrl = baris.photoUrl,
                        status = status,
                        posName = baris.posName,
                        skor = baris.skor,
                    )
                }
            }
        }.onFailure {
            Log.w(TAG, "cariDenganFoto gagal: ${it.message}")
        }.getOrDefault(emptyList())
    }

    override suspend fun hapusLaporan(reportId: String) {
        val daring = runCatching { networkMonitor.isOnline.first() }.getOrDefault(false)
        if (!daring) {
            throw IllegalStateException("Butuh koneksi internet untuk menghapus laporan.")
        }

        // Minta balik baris yang benar-benar terhapus (`select()` = PostgREST
        // `return=representation`). Tanpa ini, kebijakan RLS yang menolak DELETE
        // hanya menghasilkan "0 baris terhapus" tanpa galat, dan cache lokal akan
        // menghapus laporan yang sebenarnya masih ada di server — laporan itu lalu
        // muncul kembali pada sinkronisasi berikutnya.
        val terhapus = client.postgrest.from(TABLE).delete {
            filter { eq("id", reportId) }
            select(Columns.list("id"))
        }.decodeList<IdBaris>()

        if (terhapus.isEmpty()) {
            throw IllegalStateException(
                "Laporan tidak terhapus: bukan milik akun ini atau masih punya klaim aktif."
            )
        }

        db.withTransaction {
            antrean.deleteFor(reportId)
            laporan.deleteById(reportId)
        }
        Log.d(TAG, "hapusLaporan: $reportId dihapus dari server dan cache")
    }

    /** Baris minimal untuk memastikan server benar-benar menghapus data. */
    @Serializable
    private data class IdBaris(val id: String)

    /**
     * Satu baris keluaran RPC `cari_barang_mirip` (db/0032).
     *
     * Nama kolom mengikuti yang dikembalikan database; `status` dibiarkan teks
     * lalu diterjemahkan ke `ReportStatus` di pemanggil, agar status yang belum
     * dikenal aplikasi tidak menggagalkan seluruh pencarian.
     */
    @Serializable
    private data class CariFotoRow(
        val id: String,
        val title: String,
        @SerialName("photo_url") val photoUrl: String? = null,
        val status: String,
        @SerialName("pos_name") val posName: String? = null,
        val skor: Int = 0,
    )

    // ---------------------------------------------------------------- baca lokal
    /** Flow laporan dari Room; pembacaan pertama memicu tarikan dari server. */
    private fun aliranLaporan(): Flow<List<Report>> =
        laporan.observeAll()
            .map { baris -> baris.map { it.toDomain() } }
            .onStart { runCatching { sinkronkan() } }

    // ------------------------------------------------------------- tulis lokal

    /** Menulis perubahan pada baris yang sudah ada; dibuat PENDING agar tidak ditimpa. */
    private suspend fun ubahLokal(reportId: String, ubah: (ReportEntity) -> ReportEntity) {
        val baris = laporan.findById(reportId)
        if (baris == null) {
            Log.w(TAG, "ubahLokal: laporan $reportId tidak ada di cache, perubahan dilewati")
            return
        }
        laporan.upsert(ubah(baris))
    }

    private suspend fun antrekan(reportId: String, kind: OutboxKind, payload: String) {
        antrean.insert(
            OutboxEntity(
                kind = kind.name,
                reportId = reportId,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    // ---------------------------------------------------------------- sinkron

    /** Tarik snapshot server lalu kirim antrean lokal; aman dipanggil berkali-kali. */
    /**
     * Memaksa satu putaran sinkronisasi dari luar (mis. [LaporanSyncWorker]).
     *
     * Dipakai WorkManager saat aplikasi di latar belakang: timer 20 detik di
     * dalam proses ini tidak dapat diandalkan ketika proses dibekukan sistem.
     * Bila jaringan belum ada, fungsi ini hanya mencatat lalu kembali; antrean
     * tetap menunggu putaran berikutnya.
     */
    suspend fun sinkronkanSekarang() = sinkronkan()

    private suspend fun sinkronkan() {
        if (kunciSinkron.isLocked) return
        kunciSinkron.withLock {
            if (networkMonitor.isOnline.first().not()) {
                Log.d(TAG, "sinkron: offline, antrean menunggu")
                return
            }
            runCatching { dorongAntrean() }
                .onFailure { Log.e(TAG, "dorong antrean gagal: ${it.message}") }
            runCatching { tarikDariServer() }
                .onFailure { Log.e(TAG, "tarik laporan gagal: ${it.message}") }
        }
    }

    /** Mengirim isi `outbox` berurutan; berhenti pada kegagalan pertama agar urutan tetap. */
    private suspend fun dorongAntrean() {
        while (true) {
            val entri = antrean.first() ?: return
            var galat: String? = null
            val berhasil = runCatching { kirim(entri) }
                .onFailure {
                    galat = it.message
                    Log.e(TAG, "kirim ${entri.kind} (${entri.reportId}) gagal: ${it.message}")
                }
                .isSuccess

            if (!berhasil) {
                // Berhenti di sini: aksi berikutnya bisa bergantung pada aksi ini
                // (mis. UPDATE_STATUS setelah CREATE_REPORT), jadi urutannya dijaga.
                antrean.recordFailure(entri.id, galat)
                return
            }
            antrean.delete(entri.id)
            if (antrean.countFor(entri.reportId) == 0) laporan.markSynced(entri.reportId)
            Log.d(TAG, "kirim ${entri.kind} (${entri.reportId}) berhasil")
        }
    }

    private suspend fun kirim(entri: OutboxEntity) {
        when (OutboxKind.valueOf(entri.kind)) {
            OutboxKind.CREATE_REPORT -> {
                val isi = json.decodeFromString(LaporanPayload.serializer(), entri.payload)
                client.postgrest.from(TABLE).insert(isi)
            }

            OutboxKind.UPDATE_STATUS -> {
                val isi = json.decodeFromString(StatusPayload.serializer(), entri.payload)
                client.postgrest.from(TABLE).update({ set("status", isi.status) }) {
                    filter { eq("id", entri.reportId) }
                }
            }

            OutboxKind.ARCHIVE -> {
                val isi = json.decodeFromString(ArsipPayload.serializer(), entri.payload)
                client.postgrest.from(TABLE).update({ set("is_archived", isi.archived) }) {
                    filter { eq("id", entri.reportId) }
                }
            }

            OutboxKind.HANDOVER -> {
                // Status & pos harus berpindah bersamaan agar laporan tidak
                // berstatus "diserahkan" tanpa pos yang jelas.
                val isi = json.decodeFromString(SerahTerimaPayload.serializer(), entri.payload)
                client.postgrest.from(TABLE).update({
                    set("status", ReportStatus.DISERAHKAN_KE_POS.name)
                    set("pos_id", isi.posId)
                }) {
                    filter { eq("id", entri.reportId) }
                }
            }
        }
    }

    /** Menimpa cache lokal dengan snapshot server (baris PENDING dipertahankan). */
    private suspend fun tarikDariServer() {
        val baris = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(REPORT_SELECT_COLUMNS)) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ReportRow>()
            .mapNotNull { it.toDomain() }

        val tertunda = laporan.pendingIds().toSet()
        val aman = baris
            .filterNot { it.id in tertunda }
            .map { it.toEntity(SyncStatus.SYNCED) }

        if (aman.isNotEmpty()) {
            laporan.upsertAll(aman)
            // Laporan yang sudah tidak boleh dilihat lagi dibuang dari cache.
            // Sengaja hanya saat ada data: respons kosong bisa berarti RLS/sesi
            // bermasalah, bukan berarti semua laporan hilang.
            laporan.deleteSyncedNotIn(aman.map { it.id })
        }
        Log.d(TAG, "tarik: ${aman.size} baris (${tertunda.size} tertunda dilewati)")
    }

    private suspend fun tarikSatu(id: String) {
        val row = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(REPORT_SELECT_COLUMNS)) {
                filter { eq("id", id) }
            }
            .decodeSingleOrNull<ReportRow>() ?: return
        val domain = row.toDomain() ?: return
        if (domain.id !in laporan.pendingIds()) {
            laporan.upsert(domain.toEntity(SyncStatus.SYNCED))
        }
    }

    private companion object {
        const val TAG = "Reports"
        const val TABLE = "reports"
        const val REFRESH_INTERVAL_MS = 20_000L

        /** Panjang sidik foto MobileNetV3; harus sama dengan kolom vector(1024). */
        const val DIMENSI_VEKTOR = 1024
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

// ------------------------------------------------------------------ payload

/** Isi tabel `reports` saat membuat laporan baru (nama kolom snake_case). */
@Serializable
private data class LaporanPayload(
    val id: String,
    @kotlinx.serialization.SerialName("owner_id") val ownerId: String,
    val type: String,
    val title: String,
    val description: String,
    @kotlinx.serialization.SerialName("category_id") val categoryId: String,
    @kotlinx.serialization.SerialName("photo_url") val photoUrl: String? = null,
    @kotlinx.serialization.SerialName("last_seen_location") val lastSeenLocation: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @kotlinx.serialization.SerialName("pos_id") val posId: String? = null,
    val status: String
) {
    companion object {
        fun dari(report: Report) = LaporanPayload(
            id = report.id.ifBlank { UUID.randomUUID().toString() },
            ownerId = report.ownerId,
            type = report.type.name,
            title = report.title,
            description = report.description,
            categoryId = report.category.name,
            photoUrl = report.photoUri,
            lastSeenLocation = report.lastSeenLocation,
            latitude = report.coordinates?.latitude,
            longitude = report.coordinates?.longitude,
            posId = report.pos?.id,
            status = report.status.name
        )
    }
}

@Serializable
private data class StatusPayload(val status: String)

@Serializable
private data class ArsipPayload(val archived: Boolean)

@Serializable
private data class SerahTerimaPayload(
    val posId: String,
    val posName: String,
    val posBuilding: String
)
