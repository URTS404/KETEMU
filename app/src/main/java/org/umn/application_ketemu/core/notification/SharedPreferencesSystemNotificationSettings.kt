package org.umn.application_ketemu.core.notification

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Menyimpan sakelar notifikasi luar aplikasi di [android.content.SharedPreferences].
 *
 * Alasan memilih SharedPreferences (bukan DataStore): nilainya satu boolean kecil
 * dan harus terbaca langsung saat container dibuat — sebelum kabar pertama
 * dikirim. Proyek ini belum memakai DataStore (lihat juga `OsmMap`).
 *
 * Bawaannya [DEFAULT_ENABLED] = true sesuai permintaan produk: setelah aplikasi
 * dipasang, notifikasi luar sudah menyala bila pengguna menyetujui izin
 * POST_NOTIFICATIONS. Bila izin ditolak, layar Akun yang menurunkan sakelar ini
 * menjadi false supaya tampilan sesuai dengan kenyataan.
 */
class SharedPreferencesSystemNotificationSettings(context: Context) : SystemNotificationSettings {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(
        preferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
    )

    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        if (_enabled.value == enabled) return
        preferences.edit { putBoolean(KEY_ENABLED, enabled) }
        _enabled.value = enabled
    }

    private companion object {
        const val PREFERENCES_NAME = "ketemu_settings"
        const val KEY_ENABLED = "system_notifications_enabled"
        const val DEFAULT_ENABLED = true
    }
}
