package org.umn.application_ketemu.core.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Penanda "pengguna mengetuk notifikasi panel sistem".
 *
 * Alurnya: [AndroidSystemNotifier] menaruh extra pada intent → [org.umn.application_ketemu.MainActivity]
 * memanggil [request] → KetemuNavHost melihat [pending] bernilai `true` lalu
 * membuka layar Notifikasi dan memanggil [consume].
 *
 * Memakai `StateFlow` (bukan event sekali pakai) supaya permintaan tidak hilang
 * bila ketukan terjadi sebelum Compose sempat memasang collector — mis. saat
 * aplikasi dibuka dari keadaan mati.
 */
class NotificationTapRequests {

    private val _pending = MutableStateFlow(false)

    /** `true` selama ada permintaan yang belum dijalankan UI. */
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    /** Mencatat permintaan membuka kotak masuk. */
    fun request() {
        _pending.value = true
    }

    /** Dipanggil UI setelah navigasi dijalankan agar tidak terulang. */
    fun consume() {
        _pending.value = false
    }
}
