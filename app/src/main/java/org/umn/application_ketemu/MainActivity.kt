package org.umn.application_ketemu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import org.umn.application_ketemu.core.notification.AndroidSystemNotifier
import org.umn.application_ketemu.data.AppContainer
import org.umn.application_ketemu.ui.LocalAppContainer
import org.umn.application_ketemu.ui.navigation.KetemuAppGate
import org.umn.application_ketemu.ui.theme.KetemuTheme

/**
 * Satu-satunya Activity (single-activity architecture).
 *
 * Seluruh navigasi antar layar ditangani Compose Navigation, dan container
 * dependensi disediakan sekali di sini sehingga semua composable dapat
 * mengakses repository/network monitor lewat LocalAppContainer.
 *
 * Activity ini juga menjadi pintu masuk notifikasi panel sistem: kartu yang
 * diketuk membawa extra [AndroidSystemNotifier.EXTRA_OPEN_NOTIFICATIONS], lalu
 * permintaannya diteruskan ke [AppContainer.notificationTapRequests] agar Compose
 * membuka layar Notifikasi.
 */
class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as KetemuApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Aplikasi dibuka dari kartu notifikasi (keadaan mati / belum berjalan).
        handleNotificationTap(intent)

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                KetemuTheme {
                    KetemuAppGate()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Aplikasi sudah hidup di belakang: singleTop membuat intent baru
        // diteruskan ke sini, bukan membuat Activity kedua.
        handleNotificationTap(intent)
    }

    /**
     * Meneruskan ketukan notifikasi ke Compose. Extra dihapus supaya permintaan
     * tidak terulang saat Activity dibuat ulang (mis. rotasi layar).
     */
    private fun handleNotificationTap(intent: Intent?) {
        val openInbox = intent
            ?.getBooleanExtra(AndroidSystemNotifier.EXTRA_OPEN_NOTIFICATIONS, false)
            ?: false
        if (!openInbox) return

        intent.removeExtra(AndroidSystemNotifier.EXTRA_OPEN_NOTIFICATIONS)
        container.notificationTapRequests.request()
    }
}
