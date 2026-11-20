package org.umn.application_ketemu.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.umn.application_ketemu.data.remote.SupabaseProvider

/**
 * Penjadwal sinkronisasi laporan di latar belakang.
 *
 * Dua pekerjaan yang dipasang bersamaan:
 *  - **berkala** tiap 15 menit (batas minimum WorkManager) — menjaga antrean
 *    `outbox` tidak menumpuk saat pengguna lama tidak membuka aplikasi;
 *  - **sekali jalan** setiap aplikasi dibuka — mempercepat pengiriman laporan
 *    yang baru dibuat offline tanpa menunggu jendela 15 menit.
 *
 * Keduanya memakai kebijakan `KEEP`, jadi memanggil [jadwalkan] berkali-kali
 * (setiap `onCreate`) tidak menumpuk pekerjaan.
 */
object SinkronLaporan {

    private const val NAMA_BERKALA = "sinkron-laporan-berkala"
    private const val NAMA_SEKALI = "sinkron-laporan-sekali"

    fun jadwalkan(context: Context) {
        // Tanpa Supabase tidak ada server untuk diajak bicara.
        if (!SupabaseProvider.isConfigured) return

        val workManager = WorkManager.getInstance(context)
        val syarat = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val berkala = PeriodicWorkRequestBuilder<LaporanSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(syarat)
            .build()

        workManager.enqueueUniquePeriodicWork(
            NAMA_BERKALA,
            ExistingPeriodicWorkPolicy.KEEP,
            berkala
        )

        val sekali = OneTimeWorkRequestBuilder<LaporanSyncWorker>()
            .setConstraints(syarat)
            .build()

        // REPLACE, bukan KEEP: setiap aplikasi dibuka kita memang ingin satu
        // percobaan sinkronisasi baru. Dengan KEEP, pekerjaan sekali-jalan yang
        // sudah selesai bisa membuat permintaan berikutnya diabaikan sehingga
        // antrean tertinggal sampai jendela berkala (15 menit) berikutnya.
        workManager.enqueueUniqueWork(
            NAMA_SEKALI,
            ExistingWorkPolicy.REPLACE,
            sekali
        )
    }
}
