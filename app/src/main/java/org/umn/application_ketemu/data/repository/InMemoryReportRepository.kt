package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.repository.ReportRepository

/**
 * Penerapan sementara yang menyimpan data di memori.
 *
 * Perannya sama seperti sebuah tabel lokal pada versi final: seluruh pembacaan
 * mengalir lewat satu StateFlow sehingga UI sudah terbiasa dengan pola
 * "data berubah -> UI ikut berubah" (persis perilaku Room Flow nanti).
 */
class InMemoryReportRepository(
    initialReports: List<Report>
) : ReportRepository {

    private val reports = MutableStateFlow(initialReports)

    override fun observeReports(): Flow<List<Report>> =
        reports.map { list -> list.sortedByDescending { it.createdAt } }

    override fun observeMyReports(ownerId: String): Flow<List<Report>> =
        reports.map { list ->
            list.filter { it.ownerId == ownerId }.sortedByDescending { it.createdAt }
        }

    override suspend fun getReport(id: String): Report? =
        reports.value.firstOrNull { it.id == id }

    override suspend fun addReport(report: Report) {
        reports.value = reports.value + report
    }

    override suspend fun updateStatus(reportId: String, status: ReportStatus) {
        reports.value = reports.value.map { report ->
            if (report.id == reportId) {
                report.copy(
                    status = status,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                report
            }
        }
    }
}
