package org.umn.application_ketemu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.umn.application_ketemu.ui.LocalAppContainer
import org.umn.application_ketemu.ui.MainViewModel
import org.umn.application_ketemu.ui.di.mainViewModelFactory

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
 */
@Composable
fun KetemuAppGate(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current

    val mainViewModel: MainViewModel = viewModel(
        factory = mainViewModelFactory(container)
    )
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()

    if (!isLoggedIn) {
        AuthNavHost(modifier = modifier)
    } else {
        KetemuNavHost(modifier = modifier)
    }
}
