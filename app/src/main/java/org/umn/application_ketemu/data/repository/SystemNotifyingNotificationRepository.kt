package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.umn.application_ketemu.core.notification.SystemNotificationSettings
import org.umn.application_ketemu.core.notification.SystemNotifier
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.AppNotification
import org.umn.application_ketemu.domain.repository.NotificationRepository

/**
 * Dekorator [NotificationRepository] yang memunculkan kabar baru di panel
 * sistem Android tanpa mengubah repository aslinya (in-memory maupun Supabase).
 *
 * Dekorator dipilih supaya aturan "kapan kartu panel sistem muncul" hanya
 * ditulis di satu tempat dan tidak perlu disisipkan ke setiap ViewModel:
 *  - Pengamat memantau aliran notifikasi yang sama dengan Beranda/Notifikasi,
 *    jadi badge lonceng dan kartu panel sistem tidak pernah berbeda isi.
 *  - Hanya notifikasi MILIK pengguna aktif yang ditampilkan
 *    ([AppNotification.isVisibleTo]). Petugas yang menyetujui klaim tidak
 *    menerima kabar untuk pengklaim di perangkatnya sendiri.
 *  - Hanya notifikasi belum dibaca yang ditampilkan.
 *  - Hanya kabar BARU. Saat aplikasi dibuka, kotak masuk bisa berisi riwayat
 *    (data demo atau baris Supabase yang diambil ulang); semuanya lebih tua
 *    dari [HISTORY_GRACE_MS] sehingga tidak memicu deretan kartu lama.
 *
 * Kegagalan mengirim ke panel sistem sengaja tidak dilempar: notifikasi in-app
 * tetap tersimpan dan aplikasi tetap berjalan (mis. izin ditolak pengguna).
 *
 * Sakelar pengguna ([SystemNotificationSettings]) diperiksa di sini, bukan di
 * ViewModel: saat dimatikan, kartu yang sudah tampil dibersihkan dan kabar baru
 * tidak dikirim, tetapi kotak masuk in-app tetap utuh.
 */
class SystemNotifyingNotificationRepository(
    private val delegate: NotificationRepository,
    private val session: SessionStore,
    private val notifier: SystemNotifier,
    private val settings: SystemNotificationSettings,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : NotificationRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /** Id yang sudah pernah diproses; mencegah kartu ganda saat daftar diperbarui. */
    private val observedIds = HashSet<String>()

    /** Batas "kabar baru" — lebih tua dari ini dianggap riwayat. */
    private val oldestAcceptedTimestamp = System.currentTimeMillis() - HISTORY_GRACE_MS

    init {
        scope.launch {
            delegate.observeNotifications().collect { notifications ->
                postNewNotifications(notifications)
            }
        }
        // Mematikan sakelar langsung membersihkan kartu yang masih terpasang di
        // panel sistem; menyalakannya kembali tidak mengirim ulang kabar lama.
        scope.launch {
            settings.enabled.collect { enabled ->
                if (!enabled) runCatching { notifier.cancelAll() }
            }
        }
    }

    override fun observeNotifications(): Flow<List<AppNotification>> =
        delegate.observeNotifications()

    override suspend fun publish(notification: AppNotification) =
        delegate.publish(notification)

    override suspend fun markRead(notificationId: String) =
        delegate.markRead(notificationId)

    override suspend fun markAllRead(userId: String) =
        delegate.markAllRead(userId)

    private fun postNewNotifications(notifications: List<AppNotification>) {
        // Id tetap dicatat walau sakelar mati, supaya menyalakannya kembali tidak
        // memunculkan deretan kabar yang tiba selama notifikasi luar dimatikan.
        val fresh = notifications.filter { observedIds.add(it.id) }
        if (fresh.isEmpty() || !settings.enabled.value) return
        if (!session.isLoggedIn.value) return

        val userId = session.currentUser.value.id
        fresh.asSequence()
            .filter { it.createdAt >= oldestAcceptedTimestamp }
            .filter { it.isVisibleTo(userId) && !it.isRead }
            // Urut kronologis: kartu terbaru berakhir di atas panel sistem.
            .sortedBy { it.createdAt }
            .forEach { notification ->
                runCatching { notifier.notify(notification) }
            }
    }

    private companion object {
        /**
         * Toleransi 2 menit untuk selisih jam perangkat vs server. Notifikasi
         * yang lebih tua dari (aplikasi dijalankan - 2 menit) dianggap riwayat.
         */
        const val HISTORY_GRACE_MS = 2 * 60 * 1000L
    }
}
