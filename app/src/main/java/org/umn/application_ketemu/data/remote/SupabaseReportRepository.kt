package org.umn.application_ketemu.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.umn.application_ketemu.domain.model.GeoPoint
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.PosLocation
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.domain.repository.ReportRepository
import java.time.OffsetDateTime

/**
 * Implementasi [ReportRepository] di atas tabel `reports` (PostgREST).
 *
 * Strategi sinkronisasi tahap ini sengaja sederhana: satu cache di memori yang
 * di-refresh saat pertama dibaca dan setiap [REFRESH_INTERVAL_MS]. Ini memberi
 * sifat "UI bereaksi pada data" tanpa menambah kompleksitas Realtime. Ketika
 * Room masuk (tahap 3), cache ini digantikan query lokal dan kelas ini hanya
 * bertugas mengisi Room.
 *
 * Semua query memakai kunci publishable sehingga RLS di server yang menentukan
 * baris mana yang boleh dibaca (laporan terarsip hanya terlihat oleh STAFF/ADMIN
 * atau pemiliknya).
 */
class SupabaseReportRepository(
    private val client: SupabaseClient
) : ReportRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = MutableStateFlow<List<Report>>(emptyList())

    init {
        scope.launch {
            while (isActive) {
                runCatching { refresh() }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    /** Mengambil ulang seluruh laporan yang boleh dilihat pengguna saat ini. */
    suspend fun refresh() {
        runCatching { ambilBaru() }.onFailure {
            // Kegagalan di sini sebelumnya tidak terlihat sama sekali: cache
            // dibiarkan kosong sehingga beranda tampak "belum ada laporan".
            android.util.Log.e("Reports", "gagal muat laporan: " + it.message)
        }
    }

    private suspend fun ambilBaru() {
        val rows = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(REPORT_SELECT_COLUMNS)) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ReportRow>()
        android.util.Log.d("Reports", "refresh: baris=" + rows.size)
        cache.value = rows.mapNotNull { it.toDomain() }
    }

    override fun observeReports(): Flow<List<Report>> =
        cache.asStateFlow()
            .map { reports -> reports.filterNot { it.isArchived } }
            .onStart { runCatching { refresh() } }

    override fun observeArchivedReports(): Flow<List<Report>> =
        cache.asStateFlow()
            .map { reports -> reports.filter { it.isArchived } }
            .onStart { runCatching { refresh() } }

    override fun observeModerationQueue(): Flow<List<Report>> =
        cache.asStateFlow()
            .map { reports ->
                reports
                    .filter { !it.isArchived && it.status == ReportStatus.DILAPORKAN }
                    .sortedBy { it.createdAt }
            }
            .onStart { runCatching { refresh() } }

    override fun observeMyReports(ownerId: String): Flow<List<Report>> =
        cache.asStateFlow()
            .map { reports -> reports.filter { it.ownerId == ownerId } }
            .onStart { runCatching { refresh() } }

    override suspend fun getReport(id: String): Report? {
        val row = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(REPORT_SELECT_COLUMNS)) {
                filter { eq("id", id) }
            }
            .decodeSingleOrNull<ReportRow>()
        return row?.toDomain()
    }

    override suspend fun setArchived(reportId: String, archived: Boolean) {
        client.postgrest.from(TABLE).update({ set("is_archived", archived) }) {
            filter { eq("id", reportId) }
        }
        refresh()
    }

    override suspend fun updateStatus(reportId: String, status: ReportStatus) {
        client.postgrest.from(TABLE).update({ set("status", status.name) }) {
            filter { eq("id", reportId) }
        }
        refresh()
    }

    override suspend fun confirmHandover(reportId: String, pos: PosLocation) {
        // Satu update: status & pos tujuan harus berpindah bersamaan agar
        // laporan tidak pernah berstatus "diserahkan" tanpa pos yang jelas.
        client.postgrest.from(TABLE).update({
            set("status", ReportStatus.DISERAHKAN_KE_POS.name)
            set("pos_id", pos.id)
        }) {
            filter { eq("id", reportId) }
        }
        refresh()
    }

    override suspend fun addReport(report: Report) {
        client.postgrest.from(TABLE).insert(
            ReportInsert(
                id = report.id,
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
        )
        refresh()
    }

    private companion object {
        const val TABLE = "reports"
        const val REFRESH_INTERVAL_MS = 20_000L
    }
}

@kotlinx.serialization.Serializable
private data class ReportInsert(
    val id: String,
    @kotlinx.serialization.SerialName("owner_id") val ownerId: String,
    val type: String,
    val title: String,
    val description: String,
    @kotlinx.serialization.SerialName("category_id") val categoryId: String,
    @kotlinx.serialization.SerialName("photo_url") val photoUrl: String?,
    @kotlinx.serialization.SerialName("last_seen_location") val lastSeenLocation: String,
    val latitude: Double?,
    val longitude: Double?,
    @kotlinx.serialization.SerialName("pos_id") val posId: String?,
    val status: String
)

/** Memetakan baris PostgREST ke model domain; baris rusak dilewati (null). */
internal fun ReportRow.toDomain(): Report? {
    val parsedCreatedAt = createdAt.toEpochMillisOrNull() ?: return null
    val category = categoryId
        ?.let { runCatching { ReportCategory.valueOf(it) }.getOrNull() }
        ?: ReportCategory.LAINNYA

    // Koordinat hanya dipakai bila lengkap dan berada di rentang bumi; baris
    // dengan data GPS setengah jadi diperlakukan seperti laporan tanpa titik.
    val coordinates = latitude?.let { lat ->
        longitude?.let { lng -> GeoPoint(latitude = lat, longitude = lng).takeIf { it.isValid } }
    }

    return Report(
        id = id,
        ownerId = ownerId,
        ownerName = profiles?.fullName?.ifBlank { "Pengguna Ketemu" } ?: "Pengguna Ketemu",
        type = runCatching { ItemType.valueOf(type) }.getOrDefault(ItemType.LOST),
        title = title,
        description = description,
        category = category,
        photoUri = photoUrl,
        lastSeenLocation = lastSeenLocation,
        pos = posLocation?.let { PosLocation(id = it.id, name = it.name, building = it.building) },
        status = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.DILAPORKAN),
        syncStatus = SyncStatus.SYNCED,
        createdAt = parsedCreatedAt,
        updatedAt = updatedAt.toEpochMillisOrNull() ?: parsedCreatedAt,
        isArchived = isArchived,
        coordinates = coordinates
    )
}

private fun String?.toEpochMillisOrNull(): Long? =
    this?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
