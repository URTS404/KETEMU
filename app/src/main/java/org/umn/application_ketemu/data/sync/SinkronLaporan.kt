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
 * Penjadwal pekerjaan latar belakang aplikasi.
 *
 * Tiga pekerjaan yang dipasang bersamaan:
 *  - **sinkron laporan berkala** tiap 15 menit (batas minimum WorkManager) —
 *    menjaga antrean `outbox` tidak menumpuk saat pengguna lama tidak membuka
 *    aplikasi;
 *  - **sinkron laporan sekali jalan** setiap aplikasi dibuka — mempercepat
 *    pengiriman laporan yang baru dibuat offline tanpa menunggu jendela 15
 *    menit;
 *  - **notifikasi berkala** tiap 15 menit — menarik kotak masuk lalu
 *    memunculkan kartu panel sistem walau aplikasi sedang tertutup.
 *
 * Pekerjaan berkala memakai `KEEP` supaya memanggil [jadwalkan] berkali-kali
 * (setiap `onCreate`) tidak menumpuk pekerjaan; pekerjaan sekali jalan memakai
 * `REPLACE` karena setiap pembukaan aplikasi memang ingin percobaan baru.
 */
object SinkronLaporan {

    private const val NAMA_BERKALA = "sinkron-laporan-berkala"
    private const val NAMA_SEKALI = "sinkron-laporan-sekali"
    private const val NAMA_NOTIFIKASI = "sinkron-notifikasi-berkala"
    private const val NAMA_NOTIFIKASI_SEKALI = "sinkron-notifikasi-sekali"

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

        val notifikasi = PeriodicWorkRequestBuilder<NotifikasiSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(syarat)
            .build()

        workManager.enqueueUniquePeriodicWork(
            NAMA_NOTIFIKASI,
            ExistingPeriodicWorkPolicy.KEEP,
            notifikasi
        )

        // Satu percobaan cepat setiap aplikasi dibuka: pengguna yang baru membuka
        // aplikasi tidak perlu menunggu jendela 15 menit untuk melihat kabar baru
        // di panel notifikasi. REPLACE agar percobaan lama yang sudah selesai
        // tidak menahan percobaan berikutnya.
        val notifikasiSekali = OneTimeWorkRequestBuilder<NotifikasiSyncWorker>()
            .setConstraints(syarat)
            .build()

        workManager.enqueueUniqueWork(
            NAMA_NOTIFIKASI_SEKALI,
            ExistingWorkPolicy.REPLACE,
            notifikasiSekali
        )
    }
}
