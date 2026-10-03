package org.umn.application_ketemu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.repository.ReportRepository

/**
 * ViewModel tingkat aplikasi (dipasang di sekeliling NavHost).
 *
 * Tanggung jawabnya hanya dua: meneruskan status koneksi untuk banner offline
 * dan menyediakan peran pengguna untuk pembatasan akses menu (Staff/Admin).
 */
class MainViewModel(
    networkMonitor: NetworkMonitor,
    reportRepository: ReportRepository,
    session: SessionStore
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val pendingCount: StateFlow<Int> = reportRepository.observeReports()
        .map { reports -> reports.count { it.syncStatus == SyncStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val currentUser: StateFlow<UserProfile> = session.currentUser

    /**
     * Status sesi. Dipakai KetemuAppGate untuk menahan pengguna di halaman Login
     * (Acceptance Criteria #1 PRD: pembatasan akses per peran).
     */
    val isLoggedIn: StateFlow<Boolean> = session.isLoggedIn
}
