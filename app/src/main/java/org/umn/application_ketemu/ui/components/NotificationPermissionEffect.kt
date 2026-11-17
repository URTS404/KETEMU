package org.umn.application_ketemu.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Meminta izin POST_NOTIFICATIONS (wajib sejak Android 13) sekali saat kerangka
 * utama aplikasi dibuka — yaitu setelah pengguna berhasil masuk.
 *
 * Izin diminta di menu utama, bukan di halaman Login, supaya dialog tidak
 * mengganggu proses masuk dan alasan permintaannya sudah jelas (pengguna sudah
 * punya laporan/klaim yang akan dikabari).
 *
 * Bila ditolak, aplikasi tetap berjalan: kotak masuk in-app (lonceng Beranda)
 * tetap lengkap, hanya kartu di panel sistem yang tidak muncul. Android sendiri
 * yang mencegah dialog muncul berulang setelah ditolak dua kali.
 */
@Composable
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Hasil tidak perlu ditindaklanjuti; lihat KDoc di atas. */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
