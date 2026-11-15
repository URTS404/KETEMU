package org.umn.application_ketemu.ui.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.AuditLogEntry
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.AuditLogRepository
import org.umn.application_ketemu.domain.repository.ClaimRepository
import org.umn.application_ketemu.domain.repository.ReportRepository

/** Ringkasan angka yang ditampilkan pada dashboard moderator. */
data class AdminDashboardUiState(
    val pendingModeration: Int = 0,
    val pendingClaims: Int = 0,
    val archivedCount: Int = 0,
    val totalReports: Int = 0,
    val recentAudit: List<AuditLogEntry> = emptyList()
)

/**
 * ViewModel dashboard admin.
 *
 * Menggabungkan empat sumber data lewat `combine`, meniru pola yang nanti
 * dipakai Room (satu aliran per tabel). Tidak ada logika tulis di sini:
 * dashboard murni membaca.
 */
class AdminDashboardViewModel(
    reportRepository: ReportRepository,
    claimRepository: ClaimRepository,
    auditLogRepository: AuditLogRepository,
    session: SessionStore
) : ViewModel() {

    val isAllowed: StateFlow<Boolean> = session.currentUser
        .map { it.role.canModerate }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = session.currentUser.value.role.canModerate
        )

    /**
     * Peran aktif untuk chip pada header dashboard ("Satpam"/"Admin").
     * Dipisah dari [uiState] karena bukan angka ringkasan, melainkan identitas
     * sesi — sama seperti [isAllowed] yang juga membaca [SessionStore].
     */
    val role: StateFlow<UserRole> = session.currentUser
        .map { it.role }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = session.currentUser.value.role
        )

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        reportRepository.observeReports(),
        reportRepository.observeModerationQueue(),
        reportRepository.observeArchivedReports(),
        claimRepository.observePendingClaims(),
        auditLogRepository.observeEntries()
    ) { reports, queue, archived, claims, audit ->
        AdminDashboardUiState(
            pendingModeration = queue.size,
            pendingClaims = claims.size,
            archivedCount = archived.size,
            totalReports = reports.size + archived.size,
            recentAudit = audit.take(3)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminDashboardUiState()
    )
}
