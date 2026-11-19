package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.ChatThread
import org.umn.application_ketemu.domain.model.TemplateReply

/**
 * Percakapan pada satu laporan (lihat `db/0019_messages.sql`).
 *
 * Modelnya bukan "chat ke petugas", melainkan **chat antar-pengguna dengan
 * petugas sebagai pengawas**:
 *  - laporan HILANG : pemilik laporan (mahasiswa) ↔ penemu barang
 *  - laporan TEMUAN : penemu (pemilik laporan) ↔ pemilik barang
 *  - STAFF/ADMIN    : boleh membaca semua utas dan boleh ikut menulis
 *
 * Satu laporan + satu lawan bicara = satu utas; server yang memastikan itu
 * lewat `unique (report_id, peer_id)`, jadi menekan tombol chat berkali-kali
 * tidak membuat utas ganda.
 */
interface ChatRepository {

    /** Semua utas yang melibatkan pengguna ini (atau semua utas bila petugas). */
    fun observeThreads(): Flow<List<ChatThread>>

    /** Pesan satu utas, terlama → terbaru. */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    /**
     * Mulai atau ambil utas untuk sebuah laporan.
     *
     * @return id utas, atau null bila gagal (mis. laporan milik sendiri).
     */
    suspend fun startConversation(reportId: String): String?

    /** Kirim pesan; [isTemplate] menandai pemakaian balasan cepat petugas. */
    suspend fun send(conversationId: String, body: String, isTemplate: Boolean = false): Boolean

    /** Segarkan dari server (dipakai saat Realtime tidak tersedia). */
    suspend fun refresh()

    /** Balasan cepat yang boleh dipakai petugas. */
    fun templates(): List<TemplateReply>
}
