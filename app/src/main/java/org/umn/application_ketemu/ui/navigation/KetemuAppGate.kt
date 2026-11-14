package org.umn.application_ketemu.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.umn.application_ketemu.ui.LocalAppContainer
import org.umn.application_ketemu.ui.MainViewModel
import org.umn.application_ketemu.ui.di.mainViewModelFactory

/** Durasi animasi perpindahan Login <-> menu utama (milidetik). */
private const val MENU_ENTER_MS = 430
private const val AUTH_EXIT_MS = 300

/** Skala awal menu utama saat muncul; skala akhir halaman login saat "menjauh". */
private const val MENU_INITIAL_SCALE = 0.84f
private const val AUTH_EXIT_SCALE = 1.14f

/**
 * Gerbang aplikasi: menentukan apakah pengguna melihat halaman Login atau
 * kerangka utama (Beranda, Laporan Saya, Profil).
 *
 * Ditempatkan terpisah dari KetemuNavHost supaya aturan keamanan ini mudah
 * ditemukan dan diuji: selama sesi belum terbuka, NavHost sama sekali tidak
 * dibangun — jadi tidak ada layar internal yang bisa "bocor" dilihat pengguna
 * yang belum masuk.
 *
 * Catatan: baik gerbang maupun kerangka utama memakai kunci ViewModel yang sama
 * (MainViewModel pada cakupan Activity), sehingga status sesi konsisten.
 *
 * Perpindahan memakai [AnimatedContent] supaya saat pengguna berhasil masuk,
 * halaman Login memudar sambil membesar dan menu utama muncul dari skala sedikit
 * lebih kecil ("efek masuk aplikasi") - bukan pergantian mendadak.
 */
@Composable
fun KetemuAppGate(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current

    val mainViewModel: MainViewModel = viewModel(
        factory = mainViewModelFactory(container)
    )
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = isLoggedIn,
        transitionSpec = {
            if (targetState) {
                // Login -> menu utama: halaman masuk "membesar menjauh" lalu
                // memudar; menu utama masuk dengan zoom-in + sedikit naik.
                (fadeIn(tween(MENU_ENTER_MS)) +
                    scaleIn(tween(MENU_ENTER_MS), initialScale = MENU_INITIAL_SCALE) +
                    slideInVertically(tween(MENU_ENTER_MS)) { full -> full / 16 }) togetherWith
                    (fadeOut(tween(AUTH_EXIT_MS)) +
                        scaleOut(tween(AUTH_EXIT_MS), targetScale = AUTH_EXIT_SCALE))
            } else {
                // Keluar akun -> kembali ke halaman masuk (kebalikannya).
                (fadeIn(tween(MENU_ENTER_MS)) +
                    scaleIn(tween(MENU_ENTER_MS), initialScale = AUTH_EXIT_SCALE)
                    ) togetherWith
                    (fadeOut(tween(AUTH_EXIT_MS)) +
                        scaleOut(tween(AUTH_EXIT_MS), targetScale = MENU_INITIAL_SCALE))
            }.using(SizeTransform(clip = false))
        },
        label = "appGate",
        modifier = modifier.fillMaxSize()
    ) { loggedIn ->
        if (!loggedIn) {
            AuthNavHost(modifier = Modifier.fillMaxSize())
        } else {
            KetemuNavHost(modifier = Modifier.fillMaxSize())
        }
    }
}
