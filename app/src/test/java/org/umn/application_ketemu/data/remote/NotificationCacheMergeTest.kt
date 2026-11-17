package org.umn.application_ketemu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.application_ketemu.domain.model.AppNotification
import org.umn.application_ketemu.domain.model.AppNotificationType

/**
 * Penggabungan kabar dari Supabase Realtime ke cache kotak masuk.
 *
 * Diuji sebagai fungsi murni supaya aturan "tidak ada baris ganda" dan urutan
 * terbaru-lebih-dahulu bisa dipastikan tanpa jaringan maupun klien Supabase.
 */
class NotificationCacheMergeTest {

    private fun notification(
        id: String,
        createdAt: Long,
        isRead: Boolean = false
    ) = AppNotification(
        id = id,
        type = AppNotificationType.NEW_FOUND_ITEM,
        title = "Kabar $id",
        message = "Pesan $id",
        createdAt = createdAt,
        isRead = isRead
    )

    @Test
    fun `kabar baru masuk dan urut terbaru lebih dahulu`() {
        val lama = notification(id = "lama", createdAt = 1_000L)
        val baru = notification(id = "baru", createdAt = 2_000L)

        val hasil = listOf(lama).mergedWith(baru)

        assertEquals(listOf("baru", "lama"), hasil.map { it.id })
    }

    @Test
    fun `kabar dengan id yang sudah ada tidak digandakan`() {
        val awal = listOf(notification(id = "satu", createdAt = 1_000L))

        val hasil = awal.mergedWith(notification(id = "satu", createdAt = 1_000L))

        assertEquals(1, hasil.size)
        assertEquals("satu", hasil.single().id)
    }

    @Test
    fun `kiriman ulang tidak menimpa status sudah dibaca`() {
        val sudahDibaca = notification(id = "satu", createdAt = 1_000L, isRead = true)
        val kirimanUlang = notification(id = "satu", createdAt = 1_000L, isRead = false)

        val hasil = listOf(sudahDibaca).mergedWith(kirimanUlang)

        assertTrue(hasil.single().isRead)
    }

    @Test
    fun `daftar kosong menerima kabar pertama`() {
        val hasil = emptyList<AppNotification>()
            .mergedWith(notification(id = "pertama", createdAt = 5L))

        assertEquals(listOf("pertama"), hasil.map { it.id })
    }
}
