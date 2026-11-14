package org.umn.application_ketemu.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.repository.ConversationRepository
import org.umn.application_ketemu.domain.repository.ReportRepository

/** Angka ringkas yang ditampilkan pada kartu sapaan di Beranda. */
data class HomeStats(
    val total: Int = 0,
    val readyForPickup: Int = 0,
    val pendingSync: Int = 0
)

/** Jenis hal "baru" yang layak diberi tanda notifikasi di Beranda. */
enum class NotificationKind {
    UNREAD_MESSAGES,
    READY_FOR_PICKUP
}

/**
 * Satu baris notifikasi Beranda.
 *
 * @param reportId diisi bila notifikasi menunjuk ke satu laporan, sehingga
 *   menekannya bisa langsung membuka halaman Detail laporan tersebut.
 */
data class HomeNotification(
    val kind: NotificationKind,
    val count: Int,
    val reportId: String? = null
)

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
    session: SessionStore,
    conversationRepository: ConversationRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<ItemType?>(null)
    private val categoryFilter = MutableStateFlow<ReportCategory?>(null)

    /** Sumber status online — menggerakkan banner offline di seluruh aplikasi. */
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val currentUser: StateFlow<UserProfile> = session.currentUser

    /** Jumlah laporan yang menunggu antrean sinkron (ditampilkan pada banner). */
    val pendingCount: StateFlow<Int> = reportRepository.observeReports()
        .map { reports -> reports.count { it.syncStatus == SyncStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Ringkasan angka untuk kartu sapaan: total laporan, yang siap diambil
     * (sudah di pos), dan yang masih menunggu sinkron. Dihitung di ViewModel
     * agar layar tetap hanya bertugas menggambar.
     */
    val stats: StateFlow<HomeStats> = reportRepository.observeReports()
        .map { reports ->
            HomeStats(
                total = reports.size,
                readyForPickup = reports.count {
                    it.status == ReportStatus.SIAP_DIAMBIL ||
                        it.status == ReportStatus.DISERAHKAN_KE_POS
                },
                pendingSync = reports.count { it.syncStatus == SyncStatus.PENDING }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStats())

    /**
     * Daftar hal "baru" untuk tanda notifikasi di Beranda:
     *  - pesan yang belum dibaca pada layar Pesan,
     *  - laporan berstatus SIAP_DIAMBIL (barang sudah bisa diambil di pos).
     *
     * Dihitung di ViewModel agar layar hanya bertugas menggambar.
     */
    val notifications: StateFlow<List<HomeNotification>> = combine(
        conversationRepository.observeThreads(),
        reportRepository.observeReports()
    ) { threads, reports ->
        buildList {
            val unreadMessages = threads.sumOf { it.unreadCount }
            if (unreadMessages > 0) {
                add(HomeNotification(NotificationKind.UNREAD_MESSAGES, unreadMessages))
            }

            val ready = reports.filter { it.status == ReportStatus.SIAP_DIAMBIL }
            if (ready.isNotEmpty()) {
                add(
                    HomeNotification(
                        kind = NotificationKind.READY_FOR_PICKUP,
                        count = ready.size,
                        reportId = ready.first().id
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Memuat ulang data Beranda. Pada prototipe ini hanya memberi jeda singkat
     * sebagai umpan balik visual; tahap berikutnya akan memicu sinkronisasi.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(700)
            _isRefreshing.value = false
        }
    }

    val uiState: StateFlow<UiState<List<Report>>> = combine(
        reportRepository.observeReports(),
        searchQuery,
        typeFilter,
        categoryFilter
    ) { reports, query, type, category ->
        applyFilter(reports, query, type, category)
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

    /** Mengubah filter kategori: null = semua kategori. */
    fun onCategoryFilterChange(value: ReportCategory?) {
        categoryFilter.value = value
    }

    private fun applyFilter(
        reports: List<Report>,
        query: String,
        type: ItemType?,
        category: ReportCategory?
    ): List<Report> {
        val normalizedQuery = query.trim().lowercase()

        return reports.filter { report ->
            val matchesType = type == null || report.type == type
            val matchesCategory = category == null || report.category == category
            val matchesQuery = normalizedQuery.isEmpty() ||
                report.title.lowercase().contains(normalizedQuery) ||
                report.description.lowercase().contains(normalizedQuery) ||
                report.lastSeenLocation.lowercase().contains(normalizedQuery)

            matchesType && matchesCategory && matchesQuery
        }
    }
}
