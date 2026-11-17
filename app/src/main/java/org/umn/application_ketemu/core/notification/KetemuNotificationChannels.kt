package org.umn.application_ketemu.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import org.umn.application_ketemu.R

/**
 * Kanal notifikasi panel sistem.
 *
 * Sejak Android 8 (API 26) setiap notifikasi WAJIB menunjuk kanal yang sudah
 * terdaftar; kalau belum ada, sistem membuang notifikasi tersebut tanpa error.
 * Karena itu [ensure] dipanggil sekali di [org.umn.application_ketemu.KetemuApp]
 * dan sekali lagi sebelum mengirim (murah, idempoten).
 *
 * Satu kanal saja dipakai: seluruh kabar Ketemu (unggahan, barang temuan baru,
 * perubahan status, keputusan klaim) bertingkat kepentingan yang sama, sehingga
 * pengguna cukup punya satu sakelar di Pengaturan Android.
 */
object KetemuNotificationChannels {

    /** id kanal; stabil karena preferensi pengguna disimpan berdasarkan id ini. */
    const val UPDATES: String = "ketemu_updates"

    /** Membuat kanal [UPDATES] bila belum ada. Aman dipanggil berulang. */
    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(UPDATES) != null) return

        val channel = NotificationChannel(
            UPDATES,
            context.getString(R.string.notification_channel_name),
            // DEFAULT: muncul di panel + suara singkat, tanpa mengagetkan
            // seperti PRIORITY_HIGH (kabar ini bukan chat/panggilan).
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
