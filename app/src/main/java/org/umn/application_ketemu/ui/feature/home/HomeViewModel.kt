package org.umn.application_ketemu.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.repository.ReportRepository

/**
 * ViewModel Beranda.
 *
 * Perhatikan: seluruh state dibaca dari repository (pada versi final = Room) dan
 * dirender sebagai satu UiState. View hanya menggambar, tidak memfilter data —
 * inilah pemisahan logika UI/Bisnis/Data yang diminta PRD (MVVM).
 */
class HomeViewModel(
    reportRepository: ReportRepository,
    networkMonitor: NetworkMonitor,
    session: SessionStore
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<ItemType?>(null)

    /** Sumber status online — menggerakkan banner offline di seluruh aplikasi. */
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val currentUser: StateFlow<UserProfile> = session.currentUser

    /** Jumlah laporan yang menunggu antrean sinkron (ditampilkan pada banner). */
    val pendingCount: StateFlow<Int> = reportRepository.observeReports()
        .map { reports -> reports.count { it.syncStatus == SyncStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val uiState: StateFlow<UiState<List<Report>>> = combine(
        reportRepository.observeReports(),
        searchQuery,
        typeFilter
    ) { reports, query, type ->
        applyFilter(reports, query, type)
    }
        .map { filtered ->
            if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    /** Mengubah filter jenis: null = semua, LOST = hilang, FOUND = ditemukan. */
    fun onTypeFilterChange(value: ItemType?) {
        typeFilter.value = value
    }

    private fun applyFilter(
        reports: List<Report>,
        query: String,
        type: ItemType?
    ): List<Report> {
        val normalizedQuery = query.trim().lowercase()

        return reports.filter { report ->
            val matchesType = type == null || report.type == type
            val matchesQuery = normalizedQuery.isEmpty() ||
                report.title.lowercase().contains(normalizedQuery) ||
                report.description.lowercase().contains(normalizedQuery) ||
                report.lastSeenLocation.lowercase().contains(normalizedQuery)

            matchesType && matchesQuery
        }
    }
}
