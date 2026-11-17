package org.umn.application_ketemu.ui.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.AuditAction
import org.umn.application_ketemu.domain.model.AuditLogEntry
import org.umn.application_ketemu.domain.model.AppNotification
import org.umn.application_ketemu.domain.model.AppNotificationType
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.repository.AuditLogRepository
import org.umn.application_ketemu.domain.repository.NotificationRepository
import org.umn.application_ketemu.domain.repository.ReportRepository

/**
 * ViewModel antrean moderasi laporan.
 *
 * Dua aksi: meneruskan laporan ke pos (status [ReportStatus.DISERAHKAN_KE_POS])
 * dan mengarsipkan laporan. Setiap aksi tercatat pada audit log dan diperiksa
 * ulang terhadap peran aktif (bukan hanya menyembunyikan tombol di UI).
 */
class AdminModerationViewModel(
    private val reportRepository: ReportRepository,
    private val auditLogRepository: AuditLogRepository,
    private val notificationRepository: NotificationRepository,
    private val session: SessionStore
) : ViewModel() {

    val isAllowed: StateFlow<Boolean> = session.currentUser
        .map { it.role.canModerate }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = session.currentUser.value.role.canModerate
        )

    val queue: StateFlow<List<Report>> = reportRepository.observeModerationQueue()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun verifyAndForward(reportId: String) = viewModelScope.launch {
        val actor = session.currentUser.value
        if (!actor.role.canModerate) {
            _message.value = AdminUiText.ACCESS_DENIED
            return@launch
        }
        val report = reportRepository.getReport(reportId)
        if (report == null) {
            _message.value = AdminUiText.REPORT_NOT_FOUND
            return@launch
        }

        try {
            reportRepository.updateStatus(reportId, ReportStatus.DISERAHKAN_KE_POS)
            record(actor.id, actor.fullName, AuditAction.VERIFIKASI_LAPORAN, report.id, report.title)
            notifyOwner(
                report = report,
                actorId = actor.id,
                title = "Laporan Anda diverifikasi: ${report.title}",
                message = "Laporan \"${report.title}\" sudah diverifikasi petugas dan " +
                    "diteruskan ke pos. Bawa bukti kepemilikan saat mengambil."
            )
            _message.value = "\"${report.title}\" diverifikasi dan diteruskan ke pos."
        } catch (e: Exception) {
            android.util.Log.e("AdminModeration", "verifikasi gagal: ${e.message}", e)
            _message.value = AdminUiText.ACTION_FAILED
        }
    }

    fun archive(reportId: String) = viewModelScope.launch {
        val actor = session.currentUser.value
        if (!actor.role.canModerate) {
            _message.value = AdminUiText.ACCESS_DENIED
            return@launch
        }
        val report = reportRepository.getReport(reportId) ?: run {
            _message.value = AdminUiText.REPORT_NOT_FOUND
            return@launch
        }

        try {
            reportRepository.setArchived(reportId, archived = true)
            record(actor.id, actor.fullName, AuditAction.ARSIP_LAPORAN, report.id, report.title)
            notifyOwner(
                report = report,
                actorId = actor.id,
                title = "Laporan Anda diarsipkan",
                message = "Laporan \"${report.title}\" disembunyikan dari Beranda. " +
                    "Hubungi Pos Barang Temuan bila Anda butuh penjelasan."
            )
            _message.value = "\"${report.title}\" diarsipkan dari Beranda."
        } catch (e: Exception) {
            android.util.Log.e("AdminModeration", "arsip gagal: ${e.message}", e)
            _message.value = AdminUiText.ACTION_FAILED
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun record(
        actorId: String,
        actorName: String,
        action: AuditAction,
        targetId: String,
        note: String?
    ) {
        auditLogRepository.record(
            AuditLogEntry(
                id = UUID.randomUUID().toString(),
                actorId = actorId,
                actorName = actorName,
                action = action,
                targetType = "REPORT",
                targetId = targetId,
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Memberi tahu pemilik laporan bahwa statusnya berubah karena tindakan
     * petugas. Kegagalan notifikasi tidak boleh menggagalkan aksi moderasi,
     * jadi error-nya hanya dicatat pada log.
     */
    private suspend fun notifyOwner(
        report: Report,
        actorId: String,
        title: String,
        message: String
    ) {
        try {
            notificationRepository.publish(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    type = AppNotificationType.REPORT_STATUS_CHANGED,
                    title = title,
                    message = message,
                    createdAt = System.currentTimeMillis(),
                    reportId = report.id,
                    recipientId = report.ownerId,
                    actorId = actorId
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            android.util.Log.w("AdminModeration", "notifikasi status gagal: ${e.message}")
        }
    }
}
