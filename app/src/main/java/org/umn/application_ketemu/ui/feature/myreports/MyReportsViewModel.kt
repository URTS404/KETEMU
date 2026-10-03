package org.umn.application_ketemu.ui.feature.myreports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.repository.ReportRepository

/**
 * ViewModel "Laporan Saya".
 *
 * Menyaring laporan berdasarkan pemilik yang sedang login (bukan berdasarkan
 * input pengguna), sehingga pengguna tidak mungkin melihat riwayat orang lain.
 */
class MyReportsViewModel(
    reportRepository: ReportRepository,
    session: SessionStore
) : ViewModel() {

    val uiState: StateFlow<UiState<List<Report>>> = reportRepository
        .observeMyReports(session.currentUser.value.id)
        .map { reports ->
            if (reports.isEmpty()) UiState.Empty else UiState.Success(reports)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
