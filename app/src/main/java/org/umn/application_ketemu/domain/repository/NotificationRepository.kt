package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.umn.application_ketemu.domain.model.AppNotification

/**
 * Kotak masuk notifikasi pengguna.
 *
 * Notifikasi ditulis oleh alur kerja aplikasi (buat laporan, keputusan petugas,
 * keputusan klaim) dan dibaca oleh Beranda serta layar Notifikasi. Penyaringan
 * per pengguna dilakukan lewat [org.umn.application_ketemu.domain.model.visibleTo]
 * supaya aturan "siaran tidak untuk pembuatnya" hanya ditulis satu kali.
 */
interface NotificationRepository {

    /** Seluruh notifikasi yang tersimpan, terbaru lebih dahulu. */
    fun observeNotifications(): Flow<List<AppNotification>>

    /** Menyimpan satu notifikasi baru. Aman dipanggil walau data sudah ada. */
    suspend fun publish(notification: AppNotification)

    /** Menandai satu notifikasi sudah dibaca. */
    suspend fun markRead(notificationId: String)

    /** Menandai semua notifikasi milik [userId] sudah dibaca. */
    suspend fun markAllRead(userId: String)

    /**
     * Memuat ulang notifikasi dari server sekarang juga.
     *
     * Dipakai pekerja latar belakang (`NotifikasiSyncWorker`) yang hidup tanpa
     * UI: tanpa metode ini, kotak masuk hanya diperbarui saat layar Beranda
     * atau Notifikasi diamati, sehingga pengguna tidak diberi kabar apa pun
     * ketika aplikasi sedang tertutup.
     *
     * Implementasi demo (in-memory) tidak melakukan apa pun — tidak ada server.
     */
    suspend fun tarikSekarang() = Unit
}
