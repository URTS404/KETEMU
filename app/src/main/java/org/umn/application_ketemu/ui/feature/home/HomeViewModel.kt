package org.umn.application_ketemu.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.HasilCariFoto
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.visibleTo
import org.umn.application_ketemu.domain.repository.NotificationRepository
import org.umn.application_ketemu.domain.repository.ReportRepository

/** Angka ringkas yang ditampilkan pada kartu sapaan di Beranda. */
data class HomeStats(
    val total: Int = 0,
    val readyForPickup: Int = 0,
    val pendingSync: Int = 0
)

/**
 * ViewModel Beranda.
 *
 * Perhatikan: seluruh state dibaca dari repository (pada versi final = Room) dan
 * dirender sebagai satu UiState. View hanya menggambar, tidak memfilter data —
 * inilah pemisahan logika UI/Bisnis/Data yang diminta PRD (MVVM).
 */
class HomeViewModel(
    private val reportRepository: ReportRepository,
    networkMonitor: NetworkMonitor,
    session: SessionStore,
    private val notificationRepository: NotificationRepository,
    /**
     * Penghitung sidik rupa foto (fitur "Lens"). Disuntikkan sebagai lambda agar
     * ViewModel tidak memegang Context dan tetap bisa diuji tanpa model TFLite.
     * `null` berarti fitur cari-dengan-foto tidak tersedia di perangkat ini.
     */
    private val hitungVektorFoto: (suspend (String) -> FloatArray?)? = null
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<ItemType?>(null)
    /** Seluruh isian filter Beranda dalam satu objek (lihat [FilterBeranda]). */
    private val filter = MutableStateFlow(FilterBeranda())

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
            // Angka "Laporan" mengikuti isi Beranda: temuan yang belum diverifikasi
            // belum dihitung publik. "Menunggu sinkron" tetap dihitung dari semua
            // laporan karena itu soal unggahan perangkat pengguna sendiri.
            val published = reports.filter { it.isPublished() }
            HomeStats(
                total = published.size,
                readyForPickup = published.count {
                    it.status == ReportStatus.SIAP_DIAMBIL ||
                        it.status == ReportStatus.DISERAHKAN_KE_POS
                },
                pendingSync = reports.count { it.syncStatus == SyncStatus.PENDING }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStats())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Jumlah notifikasi tersimpan yang belum dibaca. Dipakai oleh badge lonceng
     * di Beranda, sehingga angkanya selalu sama dengan layar Notifikasi.
     */
    val unreadNotificationCount: StateFlow<Int> = combine(
        currentUser,
        notificationRepository.observeNotifications()
    ) { user, all -> all.visibleTo(user.id).count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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

    // ── Cari dengan foto ("Lens") ────────────────────────────────────────────

    private val _sedangMencariFoto = MutableStateFlow(false)

    /** True selama model dan server masih membandingkan foto. */
    val sedangMencariFoto: StateFlow<Boolean> = _sedangMencariFoto.asStateFlow()

    private val _hasilCariFoto = MutableStateFlow<List<HasilCariFoto>>(emptyList())

    /** Hasil terurut dari yang paling mirip; kosong = belum ada yang cocok. */
    val hasilCariFoto: StateFlow<List<HasilCariFoto>> = _hasilCariFoto.asStateFlow()

    private val _galatCariFoto = MutableStateFlow(false)

    /** True bila pencarian berhenti karena masalah teknis (foto gagal dibaca). */
    val galatCariFoto: StateFlow<Boolean> = _galatCariFoto.asStateFlow()

    /**
     * Mencocokkan foto galeri [uri] dengan laporan temuan berfoto.
     *
     * Alurnya: foto -> vektor (di HP, tanpa jaringan) -> RPC `cari_barang_mirip`
     * (perbandingan vektor di database). Bila perangkat sedang offline, daftar
     * hasil kosong dan pengguna tetap melihat pesan "belum ada yang mirip" —
     * bukan galat, karena memang tidak ada yang bisa dibandingkan.
     */
    fun cariDenganFoto(uri: String) {
        if (_sedangMencariFoto.value) return
        viewModelScope.launch {
            _sedangMencariFoto.value = true
            _galatCariFoto.value = false
            _hasilCariFoto.value = emptyList()
            try {
                val penghitung = hitungVektorFoto
                if (penghitung == null) {
                    Log.w(TAG_FOTO, "cariDenganFoto: penghitung vektor tidak tersedia")
                    _galatCariFoto.value = true
                    return@launch
                }
                val vektor = penghitung(uri)
                if (vektor == null) {
                    Log.w(TAG_FOTO, "cariDenganFoto: foto tidak bisa dibaca")
                    _galatCariFoto.value = true
                    return@launch
                }
                val hasil = reportRepository.cariDenganFoto(vektor)
                _hasilCariFoto.value = hasil
                Log.d(TAG_FOTO, "cariDenganFoto: ${hasil.size} hasil")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG_FOTO, "cariDenganFoto gagal: ${e.message}", e)
                _galatCariFoto.value = true
            } finally {
                _sedangMencariFoto.value = false
            }
        }
    }

    /** Menutup lembar hasil (dipanggil ketika pengguna menutup atau membuka detail). */
    fun bersihkanHasilCariFoto() {
        _hasilCariFoto.value = emptyList()
        _galatCariFoto.value = false
    }

    val uiState: StateFlow<UiState<List<Report>>> = combine(
        reportRepository.observeReports(),
        searchQuery,
        filter
    ) { reports, query, isian ->
        // Beranda hanya menampilkan laporan yang sudah "publik": laporan hilang
        // langsung tampil, sedangkan laporan temuan baru tampil setelah petugas
        // memverifikasinya (lihat [Report.isPublished]).
        applyFilter(reports.filter { it.isPublished() }, query, isian)
    }
        .map { filtered ->
            if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /**
     * Nama Pos yang punya laporan, dipakai sebagai pilihan filter "Dititipkan di Pos".
     *
     * Diambil dari seluruh laporan (bukan hasil filter) supaya pilihan Pos tidak
     * ikut menyusut saat pengguna menyaring jenis atau waktu.
     */
    val posTersedia: StateFlow<List<String>> = combine(
        reportRepository.observeReports(),
        // Diambil sekali dari tabel pos_locations; kalau jaringan gagal, daftar
        // tetap diisi dari nama Pos yang muncul di laporan.
        flow { emit(reportRepository.daftarNamaPos()) }
    ) { daftarLaporan, dariServer ->
        (dariServer + daftarLaporan.mapNotNull { it.pos?.name }).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    /** Mengubah filter jenis: null = semua, LOST = hilang, FOUND = ditemukan. */
    fun onTypeFilterChange(value: ItemType?) {
        filter.value = filter.value.copy(type = value)
    }

    /** Tombol Terapkan: seluruh isian filter diganti sekaligus. */
    fun terapkanFilter(nilai: FilterBeranda) {
        filter.value = nilai
    }

    /** Tombol Reset: semua isian dikosongkan, urutan kembali terbaru. */
    fun hapusSemuaFilter() {
        filter.value = FilterBeranda()
    }

    private fun applyFilter(
        reports: List<Report>,
        query: String,
        isian: FilterBeranda
    ): List<Report> {
        val normal = query.trim().lowercase()
        val batasWaktu = isian.waktu.millis?.let { System.currentTimeMillis() - it }

        val hasil = reports.filter { report ->
            (isian.type == null || report.type == isian.type) &&
                (isian.kategori.isEmpty() || report.category in isian.kategori) &&
                (isian.status == null || report.status == isian.status) &&
                (batasWaktu == null || report.createdAt >= batasWaktu) &&
                (isian.pos == null || report.pos?.name == isian.pos) &&
                (normal.isEmpty() ||
                    report.title.lowercase().contains(normal) ||
                    report.description.lowercase().contains(normal) ||
                    report.lastSeenLocation.lowercase().contains(normal))
        }

        // Pengurutan setelah penyaringan: terbaru berarti terbaru di antara
        // hasil filter, bukan di antara seluruh laporan.
        return when (isian.urutan) {
            Urutan.TERBARU -> hasil.sortedByDescending { it.createdAt }
            Urutan.TERLAMA -> hasil.sortedBy { it.createdAt }
            Urutan.ABJAD -> hasil.sortedBy { it.title.lowercase() }
        }
    }

    private companion object {
        const val TAG_FOTO = "Lens"
    }
}
