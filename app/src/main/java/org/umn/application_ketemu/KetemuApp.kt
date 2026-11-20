package org.umn.application_ketemu

import android.app.Application

import org.umn.application_ketemu.core.notification.KetemuNotificationChannels
import org.umn.application_ketemu.core.visual.PencariVisual
import org.umn.application_ketemu.data.AppContainer
import org.umn.application_ketemu.data.remote.SupabaseProvider
import org.umn.application_ketemu.data.sync.SinkronLaporan

/**
 * Application class milik Ketemu.
 *
 * [container] menyatukan repository, pemantau jaringan, dan sesi. Saat ini
 * disimpan sebagai properti Application agar mudah diakses ViewModel tanpa
 * framework DI; pada fase M0-Backend kelas ini akan diberi @HiltAndroidApp
 * dan isi container dipindahkan ke modul Hilt.
 */

class KetemuApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Sesi login disimpan sebelum client Supabase dipakai.
        SupabaseProvider.init(this)

        // Kanal panel sistem dibuat di awal: Android 8+ membuang notifikasi yang
        // kanalnya belum terdaftar, jadi jangan menunggu sampai kabar pertama.
        KetemuNotificationChannels.ensure(this)
        container = AppContainer(applicationContext)

        // Timer di repositori hanya hidup selama proses ini berjalan. Worker
        // berkala memastikan antrean laporan offline tetap terkirim walau
        // aplikasi sudah ditutup pengguna.
        SinkronLaporan.jadwalkan(this)

        // Sementara: memuat model visual sekali dan menjalankan satu percobaan
        // hitung agar jalur bitmap -> vektor terbukti hidup di perangkat ini
        // (lihat docs/lens-catatan.md). Dihapus setelah layar cari-dengan-foto
        // memakai model ini secara langsung.
        runCatching {
            val pencari = PencariVisual.ambil(applicationContext)
            val uji = android.graphics.Bitmap
                .createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
                .apply { eraseColor(android.graphics.Color.GRAY) }
            val vektor = pencari.vektor(uji)
            uji.recycle()
            val norma = kotlin.math.sqrt(vektor.sumOf { (it * it).toDouble() })
            android.util.Log.d(
                "Lens",
                "uji vektor: dimensi=${vektor.size} norma=${"%.3f".format(norma)}",
            )
        }.onFailure {
            android.util.Log.w("Lens", "uji vektor gagal: ${it.message}")
        }
    }

}


