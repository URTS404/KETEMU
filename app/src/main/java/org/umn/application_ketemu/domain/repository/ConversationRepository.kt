package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.ConversationThread

/**
 * Kontrak akses data percakapan.
 *
 * Pada versi final data ini datang dari Supabase (tabel messages) dan dipantau
 * lewat Realtime, sehingga ViewModel tidak perlu berubah karena hanya bergantung
 * pada interface ini (sama seperti pola [ReportRepository]).
 */
interface ConversationRepository {

    /** Seluruh percakapan, terbaru di depan. */
    fun observeThreads(): Flow<List<ConversationThread>>

    /** Pesan satu percakapan, terlama -> terbaru. */
    fun observeMessages(threadId: String): Flow<List<ChatMessage>>

    /**
     * Menambah pesan ke sebuah percakapan.
     *
     * @param fromMe true bila dikirim pengguna; false untuk balasan pihak lain
     *   (dipakai simulasi balasan otomatis pada prototipe).
     */
    suspend fun addMessage(threadId: String, text: String, fromMe: Boolean)

    /** Menandai percakapan sudah dibaca (menghapus badge belum dibaca). */
    suspend fun markRead(threadId: String)
}
