package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.umn.application_ketemu.domain.model.ReportMatch
import org.umn.application_ketemu.domain.repository.MatchRepository

/**
 * Penerapan [MatchRepository] untuk mode demo (tanpa Supabase).
 *
 * Berbeda dengan produksi, pasangan di sini tidak dihitung: data contoh sudah
 * berisi pasangan yang siap tampil di halaman detail. Penyaringan per laporan
 * dilakukan saat dibaca, sehingga satu daftar bisa dipakai dari dua arah
 * (pemilik laporan hilang maupun penemu).
 */
class InMemoryMatchRepository(
    initialMatches: List<ReportMatch> = emptyList()
) : MatchRepository {

    private val matches = MutableStateFlow(initialMatches)

    override fun observeMatchesFor(reportId: String): Flow<List<ReportMatch>> =
        matches.map { list ->
            list.filter { it.lostReportId == reportId || it.foundReportId == reportId }
                .sortedByDescending { it.matchedAt }
        }
}
