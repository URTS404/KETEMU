package org.umn.application_ketemu.ui.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.AppNotification
import org.umn.application_ketemu.domain.model.visibleTo
import org.umn.application_ketemu.domain.repository.NotificationRepository

/**
 * Isi layar Notifikasi.
 *
 * [unreadCount] ikut dikirim agar layar bisa menampilkan ringkasan dan
 * mengaktifkan tombol "Tandai dibaca" tanpa menghitung ulang di Composable.
 */
data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean get() = notifications.isEmpty()
}

/**
 * ViewModel kotak masuk notifikasi.
 *
 * Penyaringan "milik saya" diserahkan ke [visibleTo] supaya Beranda dan layar
 * ini memakai aturan yang sama (notifikasi siaran tidak muncul untuk pembuatnya).
 */
class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    session: SessionStore
) : ViewModel() {

    private val currentUser = session.currentUser

    val uiState: StateFlow<NotificationsUiState> = combine(
        currentUser,
        notificationRepository.observeNotifications()
    ) { user, all ->
        val visible = all.visibleTo(user.id)
        NotificationsUiState(
            notifications = visible,
            unreadCount = visible.count { !it.isRead },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    /** Menandai satu notifikasi sudah dibaca. */
    fun markRead(notificationId: String) {
        viewModelScope.launch { notificationRepository.markRead(notificationId) }
    }

    /** Menandai seluruh notifikasi pengguna aktif sudah dibaca. */
    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead(currentUser.value.id) }
    }
}
