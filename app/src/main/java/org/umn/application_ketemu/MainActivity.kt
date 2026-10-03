package org.umn.application_ketemu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import org.umn.application_ketemu.ui.LocalAppContainer
import org.umn.application_ketemu.ui.navigation.KetemuAppGate
import org.umn.application_ketemu.ui.theme.KetemuTheme

/**
 * Satu-satunya Activity (single-activity architecture).
 *
 * Seluruh navigasi antar layar ditangani Compose Navigation, dan container
 * dependensi disediakan sekali di sini sehingga semua composable dapat
 * mengakses repository/network monitor lewat LocalAppContainer.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as KetemuApp).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                KetemuTheme {
                    KetemuAppGate()
                }
            }
        }
    }
}
