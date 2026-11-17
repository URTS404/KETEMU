package org.umn.application_ketemu.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Satu tempat untuk menjawab "boleh tidak aplikasi ini menaruh kartu di panel
 * notifikasi HP?" — dipakai notifier sebelum mengirim, dan dipakai halaman Akun
 * untuk menampilkan peringatan bila notifikasi diblokir pengguna/sistem.
 *
 * Dua hal berbeda yang keduanya bisa mematikan kartu:
 *  - Izin runtime POST_NOTIFICATIONS (wajib sejak Android 13).
 *  - Sakelar notifikasi aplikasi di Pengaturan Android (kanal "Kabar Ketemu"
 *    dimatikan, atau seluruh notifikasi aplikasi dimatikan).
 */
object SystemNotificationAccess {

    /**
     * Nama izin runtime yang diminta lewat dialog.
     *
     * Konstanta ini di-inline compiler menjadi String biasa, sehingga aman
     * ditulis di sini walau baru dikenal sejak Android 13 — pemakaiannya selalu
     * dijaga [hasPermission] (yang selalu `true` di bawah Android 13).
     */
    @SuppressLint("InlinedApi")
    const val PERMISSION_POST_NOTIFICATIONS: String = Manifest.permission.POST_NOTIFICATIONS

    /** Apakah izin runtime POST_NOTIFICATIONS sudah diberikan. */
    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            PERMISSION_POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Apakah kartu benar-benar akan tampil (izin + sakelar sistem). */
    fun canPost(context: Context): Boolean =
        hasPermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Membuka layar notifikasi aplikasi ini di Pengaturan Android.
     *
     * Dipakai tombol bantuan pada sakelar notifikasi luar, karena setelah izin
     * ditolak dua kali Android tidak menampilkan dialog lagi — satu-satunya
     * jalan adalah lewat Pengaturan.
     */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Bila ROM tidak menyediakan layar tersebut, jangan sampai aplikasi crash.
        runCatching { context.startActivity(intent) }
    }
}
