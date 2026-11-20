package org.umn.application_ketemu.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.umn.application_ketemu.KetemuApp
import org.umn.application_ketemu.data.repository.RoomReportRepository

/**
 * Mengirim antrean `outbox` dan menarik laporan terbaru dari server di latar
 * belakang.
 *
 * Kenapa perlu: timer 20 detik di [RoomReportRepository] hanya hidup selama
 * proses aplikasi berjalan. Ketika pengguna menutup aplikasi setelah membuat
 * laporan offline, antrean tidak terkirim sampai aplikasi dibuka lagi. Worker
 * ini dijadwalkan oleh [SinkronLaporan] sehingga laporan tetap terkirim walau
 * aplikasi tidak sedang dibuka.
 *
 * Worker memakai repositori yang SAMA dengan aplikasi (lewat
 * [KetemuApp.container]) supaya tidak ada dua instance Room atau dua timer
 * yang saling menimpa.
 */
class LaporanSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val aplikasi = applicationContext as? KetemuApp
        val repositori = aplikasi?.container?.reportRepository as? RoomReportRepository

        // Tanpa Supabase, data contoh di memori tidak punya antrean untuk
        // dikirim — tidak ada gunanya mengulang pekerjaan.
        if (repositori == null || aplikasi == null) return Result.success()

        // Constraint CONNECTED hanya menjamin ada jaringan, belum tentu
        // internet yang benar-benar tervalidasi. Tanpa pemeriksaan ini worker
        // akan "berhasil" tanpa mengirim apa pun dan antrean tertinggal.
        val daring = runCatching { aplikasi.container.networkMonitor.isOnline.first() }
            .getOrDefault(false)
        if (!daring) {
            Log.d(TAG, "sinkron latar: belum ada internet, coba lagi nanti")
            return Result.retry()
        }

        return try {
            repositori.sinkronkanSekarang()
            Log.d(TAG, "sinkron latar: selesai")
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            // Kegagalan jaringan sementara: biarkan WorkManager mencoba lagi
            // dengan backoff, jangan buang antreannya.
            Log.w(TAG, "sinkron latar gagal: ${e.message}")
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "Sinkron"
    }
}
