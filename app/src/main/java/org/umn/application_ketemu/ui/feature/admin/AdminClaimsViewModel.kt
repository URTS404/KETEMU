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
import org.umn.application_ketemu.domain.model.ClaimStatus
import org.umn.application_ketemu.domain.model.Report
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

    /**
     * Klaim yang sudah disetujui tetapi barangnya belum diambil pemilik.
     *
     * Inilah antrean kerja kedua satpam: memeriksa KTM, mencocokkan kode
     * pengambilan, lalu menyerahkan barang. Setelah diserahkan, klaim keluar dari
     * daftar ini karena laporannya menjadi SELESAI_DIKLAIM (db/0028).
     */
    val readyForPickup: StateFlow<List<Claim>> = claimRepository.observeClaims()
        .map { claims ->
            claims.filter {
                it.status == ClaimStatus.DISETUJUI &&
                    it.reportStatus == ReportStatus.SIAP_DIAMBIL &&
                    it.handedOverAt == null
            }.sortedBy { it.decidedAt ?: it.createdAt }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Detail barang per id.
     *
     * Dialog tinjauan memakai peta ini untuk menampilkan deskripsi barang dan
     * pos penitipan — dua hal yang dicocokkan satpam dengan benda fisik di meja
     * pos sebelum menekan "Setujui klaim".
     */
    val reportsById: StateFlow<Map<String, Report>> = reportRepository.observeReports()
        .map { reports -> reports.associateBy { it.id } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
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
     * Menutup klaim: petugas menyerahkan barang ke pemilik.
     *
     * Server (RPC `complete_handover`, db/0028) yang memindahkan laporan temuan
     * ke SELESAI_DIKLAIM, menutup laporan hilang pasangannya, dan mengirim
     * notifikasi. Di sisi perangkat, status laporan lokal ikut diperbarui supaya
     * layar langsung berubah tanpa menunggu tarikan berkala.
     *
     * @param code kode pengambilan yang disebut pemilik; boleh kosong bila
     *   petugas memeriksa KTM secara manual.
     */
    fun completeHandover(claim: Claim, code: String? = null) = viewModelScope.launch {
        val actor = session.currentUser.value
        if (!actor.role.canModerate) {
            _message.value = AdminUiText.ACCESS_DENIED
            return@launch
        }

        try {
            val berhasil = claimRepository.completeHandover(claim.id, code)
            if (!berhasil) {
                _message.value = AdminUiText.HANDOVER_FAILED
                return@launch
            }

            // Cermin lokal: server sudah mengubah status, Room/outbox menyusul.
            reportRepository.updateStatus(claim.reportId, ReportStatus.SELESAI_DIKLAIM)

            // Catatan audit tidak boleh membatalkan serah terima yang sudah
            // terjadi di server: barang sudah berpindah tangan. Bila enum aksi
            // belum ada (migrasi db/0028 belum dijalankan), cukup dilaporkan.
            runCatching {
                auditLogRepository.record(
                    AuditLogEntry(
                        id = UUID.randomUUID().toString(),
                        actorId = actor.id,
                        actorName = actor.fullName,
                        action = AuditAction.SERAH_TERIMA_BARANG,
                        targetType = "CLAIM",
                        targetId = claim.id,
                        note = claim.reportTitle,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }.onFailure {
                android.util.Log.w("AdminClaims", "catatan audit serah terima gagal: ${it.message}")
            }

            _lastDecision.value = ClaimDecision(
                claimTitle = claim.reportTitle,
                approved = true,
                pickupCode = claim.pickupCode
            )
            _message.value = AdminUiText.HANDOVER_DONE
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            // Penolakan dari RPC diterjemahkan ke kalimat pendek: pesan PostgREST
            // mentah terlalu panjang untuk snackbar.
            val mentah = e.message.orEmpty()
            android.util.Log.e("AdminClaims", "serah terima gagal: $mentah", e)
            _message.value = when {
                mentah.contains("tidak cocok", ignoreCase = true) -> AdminUiText.HANDOVER_CODE_WRONG
                mentah.contains("belum disetujui", ignoreCase = true) -> AdminUiText.HANDOVER_NOT_APPROVED
                mentah.contains("siap diambil", ignoreCase = true) -> AdminUiText.HANDOVER_NOT_READY
                mentah.contains("petugas", ignoreCase = true) -> AdminUiText.ACCESS_DENIED
                else -> AdminUiText.HANDOVER_FAILED
            }
        }
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
