package org.umn.application_ketemu.domain.model

/**
 * Model utama yang dibaca seluruh layar (Beranda, Laporan Saya, Detail, Staff).
 *
 * Catatan arsitektur (Single Source of Truth sesuai catatan evaluator teknis PRD):
 * model ini nantinya dipetakan dari tabel Room. UI TIDAK pernah memakai objek
 * hasil response Supabase secara langsung.
 *
 * @param id dibuat di sisi klien (UUID) supaya unggahan ulang bersifat idempoten
 *           — laporan offline yang dikirim dua kali tidak akan terduplikasi.
 * @param createdAt epoch millis (perhitungan waktu relatif ada di core/util).
 */
data class Report(
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val type: ItemType,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val photoUri: String? = null,
    val lastSeenLocation: String,
    val pos: PosLocation? = null,
    val status: ReportStatus = ReportStatus.DILAPORKAN,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    /** `true` bila laporan disembunyikan moderator (tidak tampil di Beranda). */
    val isArchived: Boolean = false,
    /**
     * Titik GPS tempat barang terakhir terlihat/ditemukan.
     *
     * Opsional: `null` untuk laporan lama atau saat pengguna hanya mengisi
     * deskripsi teks [lastSeenLocation]. Kolom teks tetap dipertahankan karena
     * GPS tidak selalu tersedia (dalam gedung, izin ditolak, perangkat tanpa GPS).
     */
    val coordinates: GeoPoint? = null
) {
    /**
     * Apakah laporan ini layak tampil di Beranda publik.
     *
     * Aturan visibilitas yang disepakati:
     * - Laporan **kehilangan** langsung tampil; pemilik butuh bantuan secepatnya
     *   dan tidak ada barang fisik yang perlu diverifikasi.
     * - Laporan **temuan** baru tampil setelah petugas memverifikasi barangnya.
     *   Selama statusnya masih [ReportStatus.DILAPORKAN] barang belum dipastikan
     *   ada di Pos, jadi belum dipublikasikan.
     * - Laporan yang diarsipkan moderator selalu disembunyikan.
     *
     * Dipakai oleh Beranda ([org.umn.application_ketemu.ui.feature.home.HomeViewModel])
     * dan daftar "Mungkin barangmu" di halaman detail.
     */
    fun isPublished(): Boolean =
        !isArchived && (type == ItemType.LOST || status != ReportStatus.DILAPORKAN)
}
