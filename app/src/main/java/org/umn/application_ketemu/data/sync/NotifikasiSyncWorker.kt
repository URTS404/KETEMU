package org.umn.application_ketemu.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.umn.application_ketemu.KetemuApp

/**
 * Pekerja latar belakang yang memunculkan notifikasi panel sistem walau
 * aplikasi sedang tertutup.
 *
 * Kenapa perlu: alur di [org.umn.application_ketemu.data.repository.SystemNotifyingNotificationRepository]
 * hanya hidup selama proses aplikasi berjalan — cobanya memantau aliran
 * notifikasi dari cache. Kalau pengguna menutup aplikasi, kabar baru (keputusan
 * klaim, serah terima, pengingat pengambilan) tidak pernah sampai ke panel
 * notifikasi sampai aplikasi dibuka lagi.
 *
 * Cara kerja tiap kali dijalankan:
 *  1. lewati bila belum login, sakelar "notifikasi luar aplikasi" mati, atau
 *     server tidak dikonfigurasi;
 *  2. tarik kotak masuk terbaru dari Supabase
 *     ([org.umn.application_ketemu.domain.repository.NotificationRepository.tarikSekarang]);
 *  3. tampilkan yang belum dibaca dan lebih baru dari penanda terakhir
 *     ([PenandaNotifikasi]) supaya tidak terjadi deretan kartu lama;
 *  4. simpan penanda baru, sehingga satu notifikasi hanya berbunyi sekali.
 *
 * Batas jujurnya: WorkManager menjalankan pekerjaan berkala paling cepat tiap
 * 15 menit dan Android bisa menundanya saat mode hemat baterai. Notifikasi
 * "seketika" seperti WhatsApp/Gojek memerlukan Firebase Cloud Messaging
 * (server mengirim push), bukan pengambilan berkala seperti ini.
 */
class NotifikasiSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val aplikasi = applicationContext as? KetemuApp ?: return Result.success()
        val container = aplikasi.container

        // Sesi login dipulihkan secara asinkron saat proses aplikasi baru dibuat.
        // Pekerja ini sering dijalankan JobScheduler tepat setelah proses lahir
        // (mis. setelah aplikasi ditutup), jadi tunggu sebentar alih-alih langsung
        // menyimpulkan "belum login" dan melewatkan seluruh jadwal.
        val masuk = withTimeoutOrNull(BATAS_TUNGGU_SESI_MS) {
            container.session.isLoggedIn.first { it }
        } ?: false
        if (!masuk) {
            Log.d(TAG, "notif latar: belum ada sesi login, dilewati")
            return Result.success()
        }
        if (!container.systemNotificationSettings.enabled.value) {
            Log.d(TAG, "notif latar: sakelar notifikasi luar aplikasi mati, dilewati")
            return Result.success()
        }

        val pengguna = container.session.currentUser.value
        val penanda = PenandaNotifikasi(applicationContext)

        val hasil = runCatching {
            container.notificationRepository.tarikSekarang()
            container.notificationRepository.observeNotifications().first()
        }
        val daftar = hasil.getOrElse { galat ->
            // Jaringan mati bukan kegagalan permanen: biarkan WorkManager
            // mencoba lagi pada jadwal berikutnya.
            Log.d(TAG, "notif latar: gagal menarik kotak masuk (${galat.message}), coba lagi nanti")
            return Result.retry()
        }

        val batas = penanda.terakhirMs
        val baru = daftar
            .filter { it.createdAt > batas && !it.isRead && it.isVisibleTo(pengguna.id) }
            .sortedBy { it.createdAt }

        baru.forEach { notifikasi ->
            runCatching { container.systemNotifier.notify(notifikasi) }
        }

        // Penanda dinaikkan ke waktu notifikasi terbaru yang sudah diproses,
        // supaya kabar yang sama tidak berbunyi dua kali pada jadwal berikutnya.
        daftar.maxOfOrNull { it.createdAt }?.let { penanda.terakhirMs = it }

        if (baru.isNotEmpty()) {
            Log.d(TAG, "notif latar: ${baru.size} kabar baru ditampilkan")
        }
        return Result.success()
    }

    private companion object {
        const val TAG = "Sinkron"

        /** Cukup lama untuk pemulihan sesi Supabase, cukup singkat untuk job latar. */
        const val BATAS_TUNGGU_SESI_MS = 15_000L
    }
}
