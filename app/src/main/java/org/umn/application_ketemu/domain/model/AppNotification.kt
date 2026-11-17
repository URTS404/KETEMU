package org.umn.application_ketemu.domain.model

/**
 * Jenis notifikasi yang dikenal aplikasi.
 *
 * Setiap jenis punya padanan ikon + warna di layer UI ([AppNotificationType]
 * sengaja bebas teks tampilan supaya model domain tetap murni).
 */
enum class AppNotificationType {

    /** Konfirmasi untuk pelapor: laporan barang hilangnya sudah masuk sistem. */
    LOST_REPORT_CREATED,

    /** Konfirmasi untuk penemu: barang temuannya sudah diunggah. */
    FOUND_REPORT_CREATED,

    /** Pemberitahuan ke pengguna lain bahwa ada barang temuan baru. */
    NEW_FOUND_ITEM,

    /** Status laporan berubah karena tindakan petugas pos. */
    REPORT_STATUS_CHANGED,

    /** Klaim pengguna disetujui (berisi kode pengambilan). */
    CLAIM_APPROVED,

    /** Klaim pengguna ditolak. */
    CLAIM_REJECTED
}

/**
 * Satu notifikasi dalam kotak masuk pengguna.
 *
 * @param recipientId pemilik notifikasi; `null` berarti siaran untuk semua
 *   pengguna kecuali [actorId] (dipakai pada mode demo in-memory).
 * @param actorId pengguna yang memicu peristiwa; dipakai agar pembuat siaran
 *   tidak menerima notifikasinya sendiri.
 * @param reportId laporan terkait, bila ada. Dipakai untuk membuka detail saat
 *   notifikasi diketuk.
 */
data class AppNotification(
    val id: String,
    val type: AppNotificationType,
    val title: String,
    val message: String,
    val createdAt: Long,
    val reportId: String? = null,
    val recipientId: String? = null,
    val actorId: String? = null,
    val isRead: Boolean = false
) {

    /**
     * Apakah notifikasi ini milik [userId].
     *
     * Notifikasi bertarget hanya terlihat oleh penerimanya. Notifikasi siaran
     * terlihat oleh semua orang kecuali pembuatnya.
     */
    fun isVisibleTo(userId: String): Boolean =
        recipientId == userId || (recipientId == null && actorId != userId)
}

/**
 * Menyaring notifikasi yang boleh dilihat [userId], terbaru lebih dahulu.
 *
 * Dipakai bersama oleh Beranda dan layar Notifikasi agar hitungan badge dan
 * isi daftar selalu konsisten.
 */
fun List<AppNotification>.visibleTo(userId: String): List<AppNotification> =
    filter { it.isVisibleTo(userId) }.sortedByDescending { it.createdAt }
