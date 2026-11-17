package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.umn.application_ketemu.domain.model.AppNotification
import org.umn.application_ketemu.domain.repository.NotificationRepository

/**
 * Kotak masuk notifikasi in-memory untuk mode demo (tanpa Supabase).
 *
 * Daftar selalu diurutkan terbaru lebih dahulu agar Beranda dan layar
 * Notifikasi tidak perlu mengurutkan ulang. [markAllRead] menghormati aturan
 * visibilitas, sehingga notifikasi siaran yang dibuat pengguna lain ikut
 * tertandai selesai dibaca.
 */
class InMemoryNotificationRepository(
    initialNotifications: List<AppNotification> = emptyList()
) : NotificationRepository {

    private val notifications = MutableStateFlow(
        initialNotifications.sortedByDescending { it.createdAt }
    )

    override fun observeNotifications(): Flow<List<AppNotification>> =
        notifications.asStateFlow()

    override suspend fun publish(notification: AppNotification) {
        val current = notifications.value
        if (current.any { it.id == notification.id }) return
        notifications.value = (current + notification).sortedByDescending { it.createdAt }
    }

    override suspend fun markRead(notificationId: String) {
        notifications.value = notifications.value.map { notification ->
            if (notification.id == notificationId) {
                notification.copy(isRead = true)
            } else {
                notification
            }
        }
    }

    override suspend fun markAllRead(userId: String) {
        notifications.value = notifications.value.map { notification ->
            if (!notification.isRead && notification.isVisibleTo(userId)) {
                notification.copy(isRead = true)
            } else {
                notification
            }
        }
    }
}
