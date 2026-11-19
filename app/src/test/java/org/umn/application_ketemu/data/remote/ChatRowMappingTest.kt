package org.umn.application_ketemu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menguji pemetaan baris database menjadi model layar.
 *
 * Ini "bukti langkah pertama" yang tidak butuh jaringan: yang diperiksa adalah
 * keputusan penting di [SupabaseChatRepository] — siapa lawan bicara dilihat
 * dari sudut pandang pembaca, dan pesan siapa yang tampil di kanan.
 */
class ChatRowMappingTest {

    private val ownerId = "u-owner"
    private val peerId = "u-peer"

    private fun row() = SupabaseChatRepository.ConversationRow(
        id = "conv-1",
        reportId = "report-1",
        ownerId = ownerId,
        peerId = peerId,
        lastMessageAt = "2026-11-19T10:15:00+00:00",
        reports = SupabaseChatRepository.ReportJoin(title = "Payung Lipat Biru", type = "FOUND"),
        owner = SupabaseChatRepository.ProfileJoin(fullName = "Pak Rahmat", role = "ADMIN"),
        peer = SupabaseChatRepository.ProfileJoin(fullName = "Siti Uas", role = "USER")
    )

    @Test
    fun `pemilik laporan melihat lawan bicara sebagai peer`() {
        val thread = row().toThread(ownerId)!!

        assertEquals("Siti Uas", thread.partnerName)
        assertEquals("USER", thread.partnerRole)
        assertEquals("Payung Lipat Biru", thread.reportTitle)
    }

    @Test
    fun `pihak lain melihat lawan bicara sebagai pemilik laporan`() {
        val thread = row().toThread(peerId)!!

        assertEquals("Pak Rahmat", thread.partnerName)
        assertEquals("ADMIN", thread.partnerRole)
    }

    @Test
    fun `baris tanpa data laporan dilewati`() {
        val tanpaLaporan = row().copy(reports = null)

        assertEquals(null, tanpaLaporan.toThread(ownerId))
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
