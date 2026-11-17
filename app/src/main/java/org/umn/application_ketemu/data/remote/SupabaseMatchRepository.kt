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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportMatch
import org.umn.application_ketemu.domain.repository.MatchRepository
import java.time.OffsetDateTime

/**
 * [MatchRepository] di atas tabel `matches` (db/0018).
 *
 * Pasangan TIDAK dihitung di sini: trigger `reports_auto_match` di server yang
 * mencocokkan laporan baru dengan laporan sebaliknya, sehingga hasilnya sama
 * untuk semua pengguna dan tetap tercatat walau aplikasi sedang tertutup.
 * Kelas ini hanya membaca hasilnya, dengan pola cache + polling yang sama
 * seperti [SupabaseReportRepository] agar daftar kandidat ikut menyegar sendiri.
 *
 * Dua query dipakai:
 *  1. `matches` (disaring RLS: hanya pasangan yang menyangkut laporan pengguna),
 *  2. `reports` sekali untuk seluruh baris yang terlibat, sehingga tidak ada
 *     query per kandidat (N+1).
 *
 * Pasangan yang laporan seberangnya tidak terbaca (mis. diarsipkan moderator)
 * dibuang, supaya halaman detail tidak pernah menampilkan tautan mati.
 */
class SupabaseMatchRepository(
    private val client: SupabaseClient
) : MatchRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = MutableStateFlow<List<MatchRow>>(emptyList())

    init {
        scope.launch {
            while (isActive) {
                runCatching { refresh() }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    /** Mengambil ulang pasangan yang boleh dilihat pengguna saat ini. */
    suspend fun refresh() {
        cache.value = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(SELECT_COLUMNS)) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<MatchRow>()
    }

    override fun observeMatchesFor(reportId: String): Flow<List<ReportMatch>> =
        cache.asStateFlow()
            .map { rows ->
                rows.filter { it.lostReportId == reportId || it.foundReportId == reportId }
            }
            .map { rows -> rows.toDomain() }
            .onStart { runCatching { refresh() } }

    /** Melengkapi baris `matches` dengan laporan asli kedua sisinya. */
    private suspend fun List<MatchRow>.toDomain(): List<ReportMatch> {
        if (isEmpty()) return emptyList()

        val reports = runCatching { loadReports() }.getOrElse { return emptyList() }
        return mapNotNull { row ->
            val lost = reports[row.lostReportId] ?: return@mapNotNull null
            val found = reports[row.foundReportId] ?: return@mapNotNull null
            val matchedAt = row.createdAt.toEpochMillisOrNull() ?: return@mapNotNull null
            ReportMatch(
                id = row.id,
                lostReport = lost,
                foundReport = found,
                matchedAt = matchedAt
            )
        }.sortedByDescending { it.matchedAt }
    }

    private suspend fun loadReports(): Map<String, Report> =
        client.postgrest
            .from(REPORT_TABLE)
            .select(columns = Columns.raw(REPORT_SELECT_COLUMNS))
            .decodeList<ReportRow>()
            .mapNotNull { it.toDomain() }
            .associateBy { it.id }

    private companion object {
        const val TABLE = "matches"
        const val REPORT_TABLE = "reports"
        const val REFRESH_INTERVAL_MS = 20_000L
        const val SELECT_COLUMNS = "id,lost_report_id,found_report_id,created_at"
    }
}

@Serializable
private data class MatchRow(
    val id: String,
    @SerialName("lost_report_id") val lostReportId: String,
    @SerialName("found_report_id") val foundReportId: String,
    @SerialName("created_at") val createdAt: String? = null
)

private fun String?.toEpochMillisOrNull(): Long? =
    this?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
