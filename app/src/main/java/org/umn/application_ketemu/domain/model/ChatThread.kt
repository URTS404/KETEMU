package org.umn.application_ketemu.domain.model

/**
 * Satu utas percakapan yang ditampilkan pada layar "Pesan".
 *
 * @param partnerName nama lawan bicara (bila pengguna petugas: nama pemilik
 *   laporan; bila pengguna biasa: nama pihak lain pada laporan itu).
 * @param reportTitle judul laporan, supaya utas selalu punya konteks barang.
 * @param unreadCount jumlah pesan lawan bicara setelah terakhir dibaca.
 */
data class ChatThread(
    val id: String,
    val reportId: String,
    val reportTitle: String,
    val reportType: String,
    val partnerName: String,
    val partnerRole: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val unreadCount: Int = 0
)

/** Balasan cepat untuk petugas (opsi 5C yang dipilih user). */
data class TemplateReply(
    val label: String,
    val body: String
)
