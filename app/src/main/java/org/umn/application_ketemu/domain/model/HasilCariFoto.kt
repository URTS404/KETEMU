package org.umn.application_ketemu.domain.model

/**
 * Satu baris hasil "cari barang dengan foto".
 *
 * Dipakai Beranda setelah pengguna memilih foto dari galeri: server
 * mengembalikan laporan temuan yang sidik fotonya paling dekat, beserta
 * persentase kemiripan yang sudah dibulatkan. [skor] bulat 0..100 agar bisa
 * langsung ditampilkan tanpa pembulatan di layer UI.
 */
data class HasilCariFoto(
    val id: String,
    val title: String,
    val photoUrl: String?,
    val status: ReportStatus,
    val posName: String?,
    val skor: Int
)
