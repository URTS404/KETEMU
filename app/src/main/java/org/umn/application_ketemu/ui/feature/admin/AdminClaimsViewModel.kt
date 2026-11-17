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
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.repository.AuditLogRepository
import org.umn.application_ketemu.domain.repository.ClaimRepository
import org.umn.application_ketemu.domain.repository.NotificationRepository
import org.umn.application_ketemu.domain.repository.ReportRepository

/** Hasil terakhir keputusan klaim, dipakai layar untuk menampilkan kode pengambilan. */
data class ClaimDecision(
    val claimTitle: String,
    val approved: Boolean,
    val pickupCode: String?
)

/**
 * ViewModel antrean klaim.
 *
 * Menyetujui klaim berarti dua hal sekaligus: klaim berubah status (dan dapat
 * kode pengambilan), laporan terkait berpindah ke [ReportStatus.SIAP_DIAMBIL].
 * Keduanya dicatat di audit log sebagai satu rangkaian keputusan moderator.
 */
class AdminClaimsViewModel(
    private val claimRepository: ClaimRepository,
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

    val pendingClaims: StateFlow<List<Claim>> = claimRepository.observePendingClaims()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _lastDecision = MutableStateFlow<ClaimDecision?>(null)
    val lastDecision: StateFlow<ClaimDecision?> = _lastDecision.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun approve(claimId: String) = decide(claimId, approve = true, note = null)

    fun reject(claimId: String, note: String? = null) = decide(claimId, approve = false, note = note)

    private fun decide(claimId: String, approve: Boolean, note: String?) = viewModelScope.launch {
        val actor = session.currentUser.value
        if (!actor.role.canModerate) {
            _message.value = AdminUiText.ACCESS_DENIED
            return@launch
        }

        try {
            val claim = claimRepository.decide(
                claimId = claimId,
                approve = approve,
                decidedBy = actor.fullName,
                note = note
            ) ?: run {
                _message.value = AdminUiText.CLAIM_NOT_FOUND
                return@launch
            }

            if (approve) {
                reportRepository.updateStatus(claim.reportId, ReportStatus.SIAP_DIAMBIL)
            }

            auditLogRepository.record(
                AuditLogEntry(
                    id = UUID.randomUUID().toString(),
                    actorId = actor.id,
                    actorName = actor.fullName,
                    action = if (approve) AuditAction.SETUJUI_KLAIM else AuditAction.TOLAK_KLAIM,
                    targetType = "CLAIM",
                    targetId = claim.id,
                    note = claim.reportTitle,
                    createdAt = System.currentTimeMillis()
                )
            )

            notifyClaimant(claim, approve, actor.id)

            _lastDecision.value = ClaimDecision(
                claimTitle = claim.reportTitle,
                approved = approve,
                pickupCode = claim.pickupCode
            )
            _message.value = if (approve) {
                "Klaim disetujui. Kode pengambilan: ${claim.pickupCode}"
            } else {
                "Klaim ditolak."
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminClaims", "keputusan klaim gagal: ${e.message}", e)
            _message.value = AdminUiText.ACTION_FAILED
        }
    }

    fun clearFeedback() {
        _message.value = null
        _lastDecision.value = null
    }

    /**
     * Mengabari pengaju klaim tentang keputusannya. Kode pengambilan hanya
     * disertakan bila klaim disetujui. Notifikasi bersifat pelengkap sehingga
     * kegagalannya tidak membatalkan keputusan yang sudah tersimpan.
     */
    private suspend fun notifyClaimant(claim: Claim, approve: Boolean, actorId: String) {
        val note = claim.decisionNote?.takeIf { it.isNotBlank() }
        val message = if (approve) {
            buildString {
                append("Klaim Anda untuk \"")
                append(claim.reportTitle)
                append("\" disetujui.")
                claim.pickupCode?.let { code ->
                    append(" Kode pengambilan: ")
                    append(code)
                    append('.')
                }
                append(" Bawa KTM ke pos untuk mengambil barang.")
            }
        } else {
            buildString {
                append("Klaim Anda untuk \"")
                append(claim.reportTitle)
                append("\" belum bisa disetujui.")
                if (note != null) {
                    append(" Catatan petugas: ")
                    append(note)
                }
            }
        }

        try {
            notificationRepository.publish(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    type = if (approve) {
                        AppNotificationType.CLAIM_APPROVED
                    } else {
                        AppNotificationType.CLAIM_REJECTED
                    },
                    title = if (approve) "Klaim disetujui" else "Klaim belum disetujui",
                    message = message,
                    createdAt = System.currentTimeMillis(),
                    reportId = claim.reportId,
                    recipientId = claim.claimantId,
                    actorId = actorId
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            android.util.Log.w("AdminClaims", "notifikasi klaim gagal: ${e.message}")
        }
    }
}
