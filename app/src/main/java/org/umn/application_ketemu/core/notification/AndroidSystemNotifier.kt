package org.umn.application_ketemu.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.umn.application_ketemu.MainActivity
import org.umn.application_ketemu.R
import org.umn.application_ketemu.domain.model.AppNotification

/**
 * [SystemNotifier] memakai [NotificationManagerCompat] (berlaku Android 8 s.d.
 * versi terbaru) sehingga Kabar Ketemu tampil juga di luar aplikasi.
 *
 * Keputusan penting:
 *  - Satu kartu per notifikasi: `tag` = id notifikasi, sedangkan angka id tetap.
 *    Dua kabar berbeda jadi dua kartu; kabar yang sama tidak menumpuk.
 *  - Ketukan kartu membuka [MainActivity] (singleTop) dengan extra
 *    [EXTRA_OPEN_NOTIFICATIONS]; Compose lalu membuka layar Notifikasi.
 *  - Android 13+ mewajibkan izin POST_NOTIFICATIONS. Bila pengguna menolak,
 *    aplikasi tetap berjalan normal — kotak masuk in-app tetap lengkap.
 */
class AndroidSystemNotifier(context: Context) : SystemNotifier {

    private val appContext: Context = context.applicationContext
    private val manager: NotificationManagerCompat = NotificationManagerCompat.from(appContext)

    @SuppressLint("MissingPermission") // sudah diperiksa eksplisit di bawah
    override fun notify(notification: AppNotification) {
        if (!canPostNotifications()) return

        // Kanal dibuat ulang bila perlu (mis. aplikasi diperbarui / kanal
        // dihapus pengguna); tanpa kanal, Android 8+ membuang notifikasi.
        KetemuNotificationChannels.ensure(appContext)

        val contentIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_OPEN_INBOX,
            Intent(appContext, MainActivity::class.java).apply {
                // MainActivity memakai launchMode="singleTop", jadi intent ini
                // diteruskan lewat onNewIntent tanpa membuat layar kedua.
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val systemNotification = NotificationCompat.Builder(
            appContext,
            KetemuNotificationChannels.UPDATES
        )
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            // Isi notifikasi bisa lebih dari satu baris ("Laporan ... aman di
            // HP Anda dan akan dikirim ..."); BigTextStyle menjaga teks utuh.
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setWhen(notification.createdAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        manager.notify(notification.id, NOTIFICATION_ID, systemNotification)
    }

    private fun canPostNotifications(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        /**
         * Extra intent: pengguna mengetuk kartu notifikasi dan aplikasi harus
         * membuka layar Notifikasi (dibaca [MainActivity]).
         */
        const val EXTRA_OPEN_NOTIFICATIONS: String =
            "org.umn.application_ketemu.extra.OPEN_NOTIFICATIONS"

        /** Angka id tetap; pembeda antar kabar memakai `tag` (id notifikasi). */
        private const val NOTIFICATION_ID = 1001

        /** Request code PendingIntent; cukup satu karena extra-nya seragam. */
        private const val REQUEST_OPEN_INBOX = 2001
    }
}
