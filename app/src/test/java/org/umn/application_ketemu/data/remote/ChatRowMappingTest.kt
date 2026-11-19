package org.umn.application_ketemu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menguji pemetaan baris database menjadi model layar.
 *
 * "Bukti langkah pertama" yang tidak butuh jaringan: yang diperiksa adalah
 * keputusan penting di [SupabaseChatRepository] — nama lawan bicara sudah
 * dipilih server lewat view `chat_threads` (db/0020), dan pesan siapa yang
 * tampil di kanan.
 */
class ChatRowMappingTest {

    private val ownerId = "u-owner"
    private val peerId = "u-peer"

    private fun row() = SupabaseChatRepository.ConversationRow(
        id = "conv-1",
        reportId = "report-1",
        reportTitle = "Payung Lipat Biru",
        reportType = "FOUND",
        partnerName = "Siti Uas",
        partnerRole = "USER",
        lastMessage = "Barangnya masih ada?",
        lastMessageAt = "2026-11-19T10:15:00+00:00"
    )

    @Test
    fun `baris view dipetakan apa adanya`() {
        val thread = row().toThread(ownerId)!!

        assertEquals("Siti Uas", thread.partnerName)
        assertEquals("USER", thread.partnerRole)
        assertEquals("Payung Lipat Biru", thread.reportTitle)
        assertEquals("Barangnya masih ada?", thread.lastMessage)
        assertTrue(thread.lastMessageAt > 0L)
    }

    @Test
    fun `nama lawan bicara kosong diganti teks umum`() {
        val thread = row().copy(partnerName = "   ").toThread(peerId)!!

        assertEquals("Pengguna", thread.partnerName)
    }

    @Test
    fun `baris tanpa judul laporan dilewati`() {
        assertNull(row().copy(reportTitle = null).toThread(ownerId))
    }

    @Test
    fun `pesan sendiri ditandai fromMe dan pesan lawan tidak`() {
        val mine = SupabaseChatRepository.MessageRow(
            id = "m1",
            conversationId = "conv-1",
            senderId = peerId,
            body = "Barangnya masih ada?",
            createdAt = "2026-11-19T10:16:00+00:00"
        )
        val theirs = mine.copy(id = "m2", senderId = ownerId)

        assertTrue(mine.toDomain(peerId).fromMe)
        assertFalse(mine.toDomain(ownerId).fromMe)
        assertFalse(theirs.toDomain(peerId).fromMe)
    }

    @Test
    fun `waktu dibuat terbaca sebagai epoch millis`() {
        val message = SupabaseChatRepository.MessageRow(
            id = "m1",
            conversationId = "conv-1",
            senderId = peerId,
            body = "halo",
            createdAt = "2026-11-19T10:16:00+00:00"
        )

        assertTrue(message.toDomain(peerId).sentAt > 0L)
    }
}
