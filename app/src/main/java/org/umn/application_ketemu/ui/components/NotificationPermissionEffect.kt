package org.umn.application_ketemu.ui.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import org.umn.application_ketemu.core.notification.SystemNotificationAccess

/**
 * Meminta izin POST_NOTIFICATIONS (wajib sejak Android 13) sekali saat kerangka
 * utama aplikasi dibuka — yaitu setelah pengguna berhasil masuk.
 *
 * Izin diminta di menu utama, bukan di halaman Login, supaya dialog tidak
 * mengganggu proses masuk dan alasan permintaannya sudah jelas (pengguna sudah
 * punya laporan/klaim yang akan dikabari).
 *
 * [onPermissionResult] menerima hasil dialog. Pemanggil memakainya untuk
 * menyesuaikan sakelar di halaman Akun: bila izin ditolak, notifikasi luar
 * memang tidak akan tampil, jadi sakelarnya diturunkan agar tidak menipu.
 * Izin yang sudah diberikan tidak memicu callback (tidak ada dialog).
 *
 * Bila ditolak, aplikasi tetap berjalan: kotak masuk in-app (lonceng Beranda)
 * tetap lengkap, hanya kartu di panel sistem yang tidak muncul. Android sendiri
 * yang mencegah dialog muncul berulang setelah ditolak dua kali.
 */
@Composable
fun NotificationPermissionEffect(onPermissionResult: (Boolean) -> Unit = {}) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        if (!SystemNotificationAccess.hasPermission(context)) {
            permissionLauncher.launch(SystemNotificationAccess.PERMISSION_POST_NOTIFICATIONS)
        }
    }
}
