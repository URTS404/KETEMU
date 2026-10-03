package org.umn.application_ketemu

import android.app.Application

import org.umn.application_ketemu.data.AppContainer

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
        container = AppContainer(applicationContext)
    }

}


