package org.umn.application_ketemu.ui.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.repository.ClaimRepository
import org.umn.application_ketemu.domain.repository.MatchRepository
import org.umn.application_ketemu.domain.repository.ReportRepository

/**
 * ViewModel halaman detail barang.
 *
 * Mengamati daftar laporan lalu mengambil satu record berdasarkan id. Dengan cara
 * ini halaman detail otomatis ikut berubah saat status barang berubah (mis. staff
 * menandai "Selesai Diklaim"), tanpa perlu mekanisme refresh manual.
 *
 * [candidates] mengisi bagian "Mungkin barangmu": pasangan yang dihitung server
 * (`db/0018_auto_match.sql`) diubah menjadi laporan sisi seberang. Kandidat yang
 * belum publik (temuan belum diverifikasi), sudah selesai diklaim, atau
 * diarsipkan dibuang agar daftar hanya berisi peluang yang masih terbuka.
 *
 * [canClaim] + [submitClaim] adalah pintu masuk klaim sisi pengguna (Tahap 4):
 * pemilik menekan "Ini Barang Saya" pada laporan TEMUAN yang sudah ada di pos,
 * menuliskan ciri yang hanya diketahui pemilik, lalu klaim masuk ke antrean
 * "Verifikasi klaim" milik satpam. Klaim disimpan lewat [ClaimRepository]
 * sehingga mode offline (in-memory) dan Supabase memakai alur yang sama.
 */
class ReportDetailViewModel(
    private val reportRepository: ReportRepository,
    matchRepository: MatchRepository,
    private val claimRepository: ClaimRepository,
    private val session: SessionStore,
    private val reportId: String
) : ViewModel() {

    val uiState: StateFlow<UiState<Report>> = reportRepository.observeReports()
        .map { reports -> reports.firstOrNull { it.id == reportId } }
        .map { report ->
            if (report == null) UiState.Empty else UiState.Success(report)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** Laporan seberang yang mirip dengan laporan ini, terbaru lebih dahulu. */
    val candidates: StateFlow<List<Report>> = matchRepository.observeMatchesFor(reportId)
        .map { matches ->
            matches.mapNotNull { it.candidateFor(reportId) }
                // Hanya sarankan kandidat yang mungkin dilihat pemilik di Beranda:
                // temuan yang belum diverifikasi belum publik, jadi belum disarankan.
                .filter { candidate ->
                    candidate.isPublished() && candidate.status != ReportStatus.SELESAI_DIKLAIM
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Klaim milik pengguna aktif atas laporan ini — apa pun statusnya.
     *
     * Satu pengguna hanya boleh punya satu klaim per barang (batasan yang sama
     * dipakai tabel `claims` di Supabase lewat unique `report_id, claimant_id`),
     * sehingga hasil pertama sudah cukup untuk menentukan status di layar.
     */
    val myClaim: StateFlow<Claim?> = combine(
        claimRepository.observeClaims(),
        session.currentUser
    ) { claims, user ->
        claims.firstOrNull { it.reportId == reportId && it.claimantId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Menentukan apakah tombol "Ini Barang Saya" layak ditampilkan: barang
     * TEMUAN, sudah dititipkan di pos (atau siap diambil), masih aktif, bukan
     * laporan pengguna sendiri, belum pernah diklaim, dan pengguna bukan petugas
     * (petugas memverifikasi, bukan mengklaim).
     */
    val canClaim: StateFlow<Boolean> = combine(
        uiState,
        myClaim,
        session.currentUser
    ) { state, claim, user ->
        val report = (state as? UiState.Success)?.data ?: return@combine false
        report.type == ItemType.FOUND &&
            !report.isArchived &&
            report.status in CLAIMABLE_STATUSES &&
            report.ownerId != user.id &&
            !user.role.canModerate &&
            claim == null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Tombol "Chat penemu / pemilik" muncul untuk laporan orang lain yang masih
     * aktif — baik laporan HILANG (mengobrol dengan penemu) maupun TEMUAN
     * (mengobrol dengan pemilik). Petugas juga melihatnya untuk memantau.
     */
    val canChat: StateFlow<Boolean> = combine(
        uiState,
        session.currentUser
    ) { state, user ->
        val report = (state as? UiState.Success)?.data ?: return@combine false
        !report.isArchived && report.ownerId != user.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _message = MutableStateFlow<String?>(null)
    /** Pesan sekali tampil untuk snackbar (berhasil/gagal mengirim klaim). */
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Mengirim klaim barang temuan atas nama pengguna aktif.
     *
     * Semua syarat diperiksa ulang di sini (bukan hanya di UI) supaya aksi
     * ganda — dua kali tekan, layar usang, atau pemanggilan langsung — tidak
     * pernah menghasilkan klaim ganda atau klaim atas barang sendiri.
     */
    fun submitClaim(answer: String) = viewModelScope.launch {
        val actor = session.currentUser.value
        val report = (uiState.value as? UiState.Success)?.data

        when {
            report == null -> {
                _message.value = ClaimUiText.REPORT_NOT_FOUND
                return@launch
            }

            report.ownerId == actor.id -> {
                _message.value = ClaimUiText.OWN_REPORT
                return@launch
            }

            report.type != ItemType.FOUND ||
                report.isArchived ||
                report.status !in CLAIMABLE_STATUSES -> {
                _message.value = ClaimUiText.NOT_AT_POS
                return@launch
            }

            myClaim.value != null -> {
                _message.value = ClaimUiText.ALREADY_SUBMITTED
                return@launch
            }

            answer.trim().length < ClaimUiText.MIN_ANSWER_LENGTH -> {
                _message.value = ClaimUiText.ANSWER_TOO_SHORT
                return@launch
            }
        }

        try {
            claimRepository.submit(
                Claim(
                    id = UUID.randomUUID().toString(),
                    reportId = report.id,
                    reportTitle = report.title,
                    claimantId = actor.id,
                    claimantName = actor.fullName,
                    claimantEmail = actor.email,
                    answer = answer.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
            _message.value = ClaimUiText.SUBMITTED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            android.util.Log.e("ReportDetail", "pengajuan klaim gagal: ${e.message}", e)
            // Pelanggaran indeks unik parsial (db/0029) berarti klaim aktif sudah
            // ada — biasanya karena dua percobaan cepat beruntun atau layar lain
            // sudah mengajukan lebih dulu. Pesan kuncinya tidak layak ditampilkan.
            _message.value = if (e.isDuplicateClaim()) {
                ClaimUiText.ALREADY_ACTIVE
            } else {
                ClaimUiText.SUBMIT_FAILED
            }
        }
    }

    /**
     * Laporan ini milik pengguna aktif? Hanya pemilik yang boleh menghapus.
     *
     * Tombol hapus sengaja tidak ditampilkan untuk petugas: penanganan petugas
     * adalah *Arsipkan* (masih bisa dipulihkan dan tetap tercatat di audit),
     * bukan menghapus permanen milik orang lain.
     */
    val isOwner: StateFlow<Boolean> = combine(
        uiState,
        session.currentUser
    ) { state, user ->
        val report = (state as? UiState.Success)?.data ?: return@combine false
        report.ownerId == user.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _sedangHapus = MutableStateFlow(false)

    /** Menandai proses hapus berjalan agar tombol tidak ditekan dua kali. */
    val sedangHapus: StateFlow<Boolean> = _sedangHapus.asStateFlow()

    private val _terhapus = MutableStateFlow(false)

    /** Bernilai true setelah laporan benar-benar hilang; layar detail lalu ditutup. */
    val terhapus: StateFlow<Boolean> = _terhapus.asStateFlow()

    /**
     * Menghapus laporan milik pengguna.
     *
     * Dipakai ketika laporan salah kirim atau barang hilangnya sudah ditemukan
     * sehingga tidak perlu dicari lagi. Server menolak bila masih ada klaim
     * aktif, dan penolakan itu diterjemahkan menjadi pesan yang bisa dibaca
     * pengguna (bukan kode PostgREST).
     */
    fun hapusLaporan() {
        if (_sedangHapus.value) return
        _sedangHapus.value = true
        viewModelScope.launch {
            try {
                reportRepository.hapusLaporan(reportId)
                _terhapus.value = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                android.util.Log.e("ReportDetail", "hapus laporan gagal: ${e.message}", e)
                _message.value = if (e.isActiveClaimBlockingDelete()) {
                    ClaimUiText.DELETE_BLOCKED_BY_CLAIM
                } else {
                    ClaimUiText.DELETE_FAILED
                }
            } finally {
                _sedangHapus.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private companion object {
        /** Status laporan yang barangnya sudah berada di pos, jadi boleh diklaim. */
        val CLAIMABLE_STATUSES = setOf(
            ReportStatus.DISERAHKAN_KE_POS,
            ReportStatus.SIAP_DIAMBIL
        )
    }
}

/**
 * Apakah kegagalan ini berasal dari indeks unik klaim aktif (db/0029)?
 *
 * PostgREST membalas kode `23505` dengan pesan yang menyebut nama indeksnya,
 * jadi dua penanda itu dipakai. Pemeriksaan sengaja longgar karena pesan bisa
 * berubah antar versi Supabase.
 */
private fun Throwable.isDuplicateClaim(): Boolean {
    val pesan = message.orEmpty()
    return pesan.contains("23505") ||
        pesan.contains("claims_one_active_per_user") ||
        pesan.contains("claims_unique_active")
}

/**
 * Apakah penolakan hapus ini karena laporan masih punya klaim aktif?
 *
 * Kebijakan RLS `reports_delete_own` (db/0031) hanya mengizinkan hapus bila
 * tidak ada klaim `MENUNGGU`/`DISETUJUI`. PostgREST melaporkannya sebagai
 * pelanggaran RLS (`42501`) atau sebagai nol baris terhapus tanpa galat — jadi
 * pengguna diberi pesan "masih diproses petugas" agar tidak bingung.
 */
private fun Throwable.isActiveClaimBlockingDelete(): Boolean {
    val pesan = message.orEmpty()
    return pesan.contains("42501") ||
        pesan.contains("row-level security") ||
        pesan.contains("reports_delete_own") ||
        // Penjaga di RoomReportRepository saat server melaporkan "0 baris terhapus":
        // kebijakan RLS belum ada, laporan bukan milik akun ini, atau klaim masih aktif.
        pesan.contains("tidak terhapus")
}
