package org.umn.application_ketemu.ui.feature.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.GeoPoint
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.domain.repository.ReportRepository
import java.util.UUID

/** Hasil simpan laporan, dipakai UI untuk menampilkan pesan yang tepat. */
data class SaveResult(
    val savedOffline: Boolean,
    val reportTitle: String
)

/**
 * Status pengiriman laporan untuk umpan balik layar.
 *
 * Dipisahkan dari [SaveResult] agar UI bisa membedakan "sedang diproses" dan
 * "gagal", dua keadaan yang tidak mungkin diwakili oleh hasil akhir saja.
 */
sealed interface SaveUiState {

    data object Idle : SaveUiState

    data object Saving : SaveUiState

    data class Success(val result: SaveResult) : SaveUiState

    /** Kegagalan penyimpanan; data formulir tetap ada sehingga bisa diulang. */
    data object Error : SaveUiState
}

/** Data formulir yang disimpan sementara agar tombol "Coba Lagi" bisa mengulang. */
private data class PendingReport(
    val type: ItemType,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val location: String,
    val coordinates: GeoPoint?,
    val photoUri: String?
)

/**
 * ViewModel pembuatan laporan.
 *
 * Menegakkan dua aturan penting PRD:
 *  1. Laporan SELALU tersimpan lebih dulu secara lokal (tidak pernah gagal
 *     karena tidak ada internet).
 *  2. Status sinkronisasi ditentukan oleh kondisi jaringan saat menyimpan:
 *     online  -> SYNCED (dianggap sudah dikirim ke server)
 *     offline -> PENDING (menunggu WorkManager pada fase M4)
 */
class CreateReportViewModel(
    private val reportRepository: ReportRepository,
    networkMonitor: NetworkMonitor,
    private val session: SessionStore
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveUiState>(SaveUiState.Idle)
    val saveState: StateFlow<SaveUiState> = _saveState.asStateFlow()

    /** Disimpan sampai berhasil supaya kegagalan bisa dicoba ulang tanpa isi ulang. */
    private var pendingReport: PendingReport? = null

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun saveReport(
        type: ItemType,
        title: String,
        description: String,
        category: ReportCategory,
        location: String,
        coordinates: GeoPoint?,
        photoUri: String?
    ) {
        if (title.isBlank() || location.isBlank()) return
        if (_saveState.value == SaveUiState.Saving ||
            _saveState.value is SaveUiState.Success
        ) {
            return
        }

        val input = PendingReport(
            type = type,
            title = title,
            description = description,
            category = category,
            location = location,
            coordinates = coordinates,
            photoUri = photoUri
        )
        pendingReport = input
        startSave(input)
    }

    /**
     * Mengulang pengiriman dengan isi formulir yang sama setelah gagal.
     * Formulir tidak perlu diisi ulang sehingga kesalahan jaringan sesaat
     * tidak menghilangkan data pengguna.
     */
    fun retryLastSave() {
        val input = pendingReport ?: return
        startSave(input)
    }

    /** Menutup dialog gagal dan kembali ke formulir tanpa menghapus isinya. */
    fun dismissSaveError() {
        if (_saveState.value is SaveUiState.Error) {
            _saveState.value = SaveUiState.Idle
        }
    }

    private fun startSave(input: PendingReport) {
        if (_saveState.value == SaveUiState.Saving ||
            _saveState.value is SaveUiState.Success
        ) {
            return
        }
        _saveState.value = SaveUiState.Saving

        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            try {
                val online = isOnline.first()
                val now = System.currentTimeMillis()
                val owner = session.currentUser.value

                val report = Report(
                    // UUID dibuat di klien supaya pengiriman ulang bersifat idempoten.
                    id = UUID.randomUUID().toString(),
                    ownerId = owner.id,
                    ownerName = owner.fullName,
                    type = input.type,
                    title = input.title.trim(),
                    description = input.description.trim(),
                    category = input.category,
                    photoUri = input.photoUri,
                    lastSeenLocation = input.location.trim(),
                    // Titik GPS hanya disimpan bila valid; kolom teks lokasi tetap
                    // menjadi deskripsi utama yang dibaca petugas.
                    coordinates = input.coordinates?.takeIf { it.isValid },
                    status = ReportStatus.DILAPORKAN,
                    syncStatus = if (online) SyncStatus.SYNCED else SyncStatus.PENDING,
                    createdAt = now
                )

                reportRepository.addReport(report)
                waitForMinimumFeedback(startedAt)

                pendingReport = null
                _saveState.value = SaveUiState.Success(
                    SaveResult(
                        savedOffline = !online,
                        reportTitle = report.title
                    )
                )
            } catch (cancelled: CancellationException) {
                // Jangan ubah pembatalan coroutine menjadi pesan gagal palsu.
                throw cancelled
            } catch (_: Exception) {
                // Pesan teknis tidak ditampilkan; pengguna hanya perlu tahu
                // bahwa laporan belum terkirim dan bisa mencoba lagi.
                waitForMinimumFeedback(startedAt)
                _saveState.value = SaveUiState.Error
            }
        }
    }

    /**
     * Penyimpanan lokal bisa selesai dalam hitungan milidetik. Jeda minimum
     * kecil ini membuat animasi "sedang memproses" sempat terlihat, sehingga
     * perubahan status tidak terasa seperti lompatan yang membingungkan.
     */
    private suspend fun waitForMinimumFeedback(startedAt: Long) {
        val elapsed = System.currentTimeMillis() - startedAt
        val remaining = MIN_SAVING_FEEDBACK_MS - elapsed
        if (remaining > 0) delay(remaining)
    }

    private companion object {
        const val MIN_SAVING_FEEDBACK_MS = 450L
    }
}
