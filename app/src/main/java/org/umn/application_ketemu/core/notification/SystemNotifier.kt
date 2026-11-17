package org.umn.application_ketemu.core.notification

import org.umn.application_ketemu.domain.model.AppNotification

/**
 * Pengirim notifikasi ke panel sistem Android (notification tray).
 *
 * Kotak masuk in-app tetap menjadi sumber kebenaran; [SystemNotifier] hanya
 * memunculkan kabar yang sama di luar aplikasi supaya pengguna tidak perlu
 * membuka Ketemu untuk mengetahui ada laporan/klaim baru.
 *
 * Dipisahkan sebagai interface agar lapisan data bisa diuji tanpa
 * `NotificationManager` — unit test memakai implementasi palsu yang mencatat
 * notifikasi yang dikirim.
 */
interface SystemNotifier {

    /** Menampilkan [notification] pada panel notifikasi perangkat. */
    fun notify(notification: AppNotification)
}
