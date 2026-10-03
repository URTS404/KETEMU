package org.umn.application_ketemu.ui

import androidx.compose.runtime.staticCompositionLocalOf
import org.umn.application_ketemu.data.AppContainer

/**
 * Menyediakan [AppContainer] ke seluruh composable tanpa perlu mengoper
 * dependensi lewat parameter (menghindari "prop drilling").
 *
 * Diisi sekali pada MainActivity dan digantikan oleh Hilt pada fase berikutnya.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error(
        "AppContainer belum tersedia. Bungkus UI dengan " +
            "CompositionLocalProvider(LocalAppContainer provides container) { ... }"
    )
}
