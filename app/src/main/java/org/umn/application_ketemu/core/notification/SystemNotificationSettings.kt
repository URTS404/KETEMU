package org.umn.application_ketemu.core.notification

import kotlinx.coroutines.flow.StateFlow

/**
 * Sakelar "notifikasi luar aplikasi" milik pengguna (tersimpan di perangkat).
 *
 * Dipisahkan sebagai interface dengan dua alasan:
 *  - Dekorator notifikasi ([org.umn.application_ketemu.data.repository.SystemNotifyingNotificationRepository])
 *    bisa diuji tanpa Android — unit test memakai implementasi palsu.
 *  - Penyimpanannya bisa diganti (mis. DataStore) tanpa menyentuh UI/dekorator.
 *
 * Nilai bawaan implementasi produksi adalah `true`: begitu aplikasi dipasang,
 * notifikasi luar aktif selama pengguna memberi izin POST_NOTIFICATIONS.
 */
interface SystemNotificationSettings {

    /** `true` = kartu panel sistem boleh muncul untuk kabar baru. */
    val enabled: StateFlow<Boolean>

    /** Menyimpan pilihan pengguna dari halaman Akun > Pengaturan. */
    fun setEnabled(enabled: Boolean)
}
