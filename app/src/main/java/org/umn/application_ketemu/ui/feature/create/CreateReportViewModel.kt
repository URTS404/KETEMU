package org.umn.application_ketemu.ui.feature.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.session.SessionStore
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

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _lastSaveResult = MutableStateFlow<SaveResult?>(null)
    val lastSaveResult: StateFlow<SaveResult?> = _lastSaveResult.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun saveReport(
        type: ItemType,
        title: String,
        description: String,
        category: ReportCategory,
        location: String,
        photoUri: String?
    ) {
        if (title.isBlank() || location.isBlank()) return

        viewModelScope.launch {
            val online = isOnline.first()
            val now = System.currentTimeMillis()
            val owner = session.currentUser.value

            val report = Report(
                // UUID dibuat di klien supaya pengiriman ulang bersifat idempoten.
                id = UUID.randomUUID().toString(),
                ownerId = owner.id,
                ownerName = owner.fullName,
                type = type,
                title = title.trim(),
                description = description.trim(),
                category = category,
                photoUri = photoUri,
                lastSeenLocation = location.trim(),
                status = ReportStatus.DILAPORKAN,
                syncStatus = if (online) SyncStatus.SYNCED else SyncStatus.PENDING,
                createdAt = now
            )

            reportRepository.addReport(report)
            _lastSaveResult.value = SaveResult(
                savedOffline = !online,
                reportTitle = report.title
            )
            _isSaved.value = true
        }
    }

    /** Dipanggil UI setelah pesan hasil ditampilkan agar tidak muncul dua kali. */
    fun consumeSaveResult() {
        _lastSaveResult.value = null
    }
}
