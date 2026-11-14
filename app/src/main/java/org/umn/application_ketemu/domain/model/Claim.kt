package org.umn.application_ketemu.domain.model

/**
 * Pengajuan klaim atas sebuah laporan temuan.
 *
 * @param answer jawaban atas pertanyaan verifikasi yang hanya diketahui pemilik
 *   barang (mis. ciri tersembunyi). Disimpan agar petugas dapat menilai.
 * @param pickupCode kode pengambilan 6 karakter, dibuat saat klaim disetujui.
 */
data class Claim(
    val id: String,
    val reportId: String,
    val reportTitle: String,
    val claimantId: String,
    val claimantName: String,
    val claimantEmail: String,
    val answer: String,
    val status: ClaimStatus = ClaimStatus.MENUNGGU,
    val createdAt: Long,
    val decidedAt: Long? = null,
    val decidedBy: String? = null,
    val decisionNote: String? = null,
    val pickupCode: String? = null
)
