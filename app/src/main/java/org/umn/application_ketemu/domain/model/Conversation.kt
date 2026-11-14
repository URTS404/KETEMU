package org.umn.application_ketemu.domain.model

/**
 * Satu percakapan antara pengguna dengan pihak lain (penemu / pemilik barang).
 *
 * Selalu terhubung ke satu laporan supaya konteksnya jelas: pengguna tahu
 * percakapan ini tentang barang yang mana.
 */
data class ConversationThread(
    val id: String,
    val partnerName: String,
    val partnerRole: String,
    val reportId: String,
    val reportTitle: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val unreadCount: Int = 0
)

/** Satu pesan di dalam sebuah percakapan. */
data class ChatMessage(
    val id: String,
    val threadId: String,
    val text: String,
    /** true = pesan dari pengguna yang sedang masuk (ditampilkan di kanan). */
    val fromMe: Boolean,
    val sentAt: Long
)
