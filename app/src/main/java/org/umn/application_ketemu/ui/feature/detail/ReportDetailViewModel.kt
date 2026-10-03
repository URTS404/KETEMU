package org.umn.application_ketemu.ui.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.repository.ReportRepository

/**
 * ViewModel halaman detail barang.
 *
 * Mengamati daftar laporan lalu mengambil satu record berdasarkan id. Dengan cara
 * ini halaman detail otomatis ikut berubah saat status barang berubah (mis. staff
 * menandai "Selesai Diklaim"), tanpa perlu mekanisme refresh manual.
 */
class ReportDetailViewModel(
    reportRepository: ReportRepository,
    private val reportId: String
) : ViewModel() {

    val uiState: StateFlow<UiState<Report>> = reportRepository.observeReports()
        .map { reports -> reports.firstOrNull { it.id == reportId } }
        .map { report ->
            if (report == null) UiState.Empty else UiState.Success(report)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
