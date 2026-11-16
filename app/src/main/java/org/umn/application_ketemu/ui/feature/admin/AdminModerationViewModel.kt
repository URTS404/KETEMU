package org.umn.application_ketemu.ui.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
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
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.repository.AuditLogRepository
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
}
