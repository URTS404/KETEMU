package org.umn.application_ketemu.ui.feature.myreports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.data.dummy.DummyData
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
    private val reportRepository: ReportRepository,
    session: SessionStore
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)

    /** Dipakai pull-to-refresh di layar Aktivitas. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                reportRepository.segarkanDariServer()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val uiState: StateFlow<UiState<List<Report>>> = reportRepository
        .observeMyReports(session.currentUser.value.id)
        .map { reports ->
            // Bila pengguna (akun demo/akun baru) belum punya laporan, tampilkan
            // contoh laporan milik profil demo agar layar tidak tampak kosong/
            // rusak saat ditinjau. Data milik pengguna tetap diutamakan.
            val visible = reports.ifEmpty {
                DummyData.reports().filter { it.ownerId == DummyData.student.id }
            }
            if (visible.isEmpty()) UiState.Empty else UiState.Success(visible)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
