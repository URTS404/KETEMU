package org.umn.application_ketemu

import android.app.Application

import org.umn.application_ketemu.core.notification.KetemuNotificationChannels
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
    }

}


