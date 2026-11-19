package org.umn.application_ketemu.domain.model

/** Status pengiriman sebuah pesan chat. */
enum class MessageStatus {
    /** Belum sampai ke server (menunggu jaringan). */
    SENDING,

    /** Sudah tersimpan di server. */
    SENT,

    /** Gagal setelah beberapa percobaan; pengguna bisa menekan untuk mencoba lagi. */
    FAILED
}

/**
 * Satu pesan pada sebuah percakapan.
 *
 * @param status pesan yang belum terkirim tetap ditampilkan (seperti WhatsApp)
 *   supaya pengguna melihat bahwa pesannya tidak hilang saat offline.
 */
data class ChatMessage(
    val id: String,
    val threadId: String,
    val text: String,
    /** true = pesan dari pengguna yang sedang masuk (ditampilkan di kanan). */
    val fromMe: Boolean,
    val sentAt: Long,
    val status: MessageStatus = MessageStatus.SENT
)

/**
 * Satu utas percakapan versi mode lokal (tanpa Supabase).
 *
 * Dipakai `InMemoryConversationRepository` dan data contoh di `DummyData`,
 * sehingga aplikasi tetap bisa didemokan tanpa jaringan.
 */
data class ConversationThread(
    val id: String,
    val reportId: String = "",
    val reportTitle: String,
    val partnerName: String,
    val partnerRole: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val unreadCount: Int = 0
)
