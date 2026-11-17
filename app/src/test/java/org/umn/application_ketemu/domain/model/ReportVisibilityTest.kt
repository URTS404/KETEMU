package org.umn.application_ketemu.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aturan tampil di Beranda ([Report.isPublished]).
 *
 * Fungsi ini menentukan kapan laporan boleh dilihat publik, jadi diuji langsung
 * tanpa UI: laporan hilang langsung tampil, laporan temuan menunggu verifikasi
 * petugas, dan laporan yang diarsipkan selalu disembunyikan.
 */
class ReportVisibilityTest {

    private fun report(
        type: ItemType,
        status: ReportStatus = ReportStatus.DILAPORKAN,
        archived: Boolean = false
    ) = Report(
        id = "rpt-test",
        ownerId = "user-test",
        ownerName = "Penguji",
        type = type,
        title = "Barang Uji",
        description = "",
        category = ReportCategory.AKSESORIS,
        lastSeenLocation = "Kampus",
        status = status,
        createdAt = 1_700_000_000_000L,
        isArchived = archived
    )

    @Test
    fun `laporan hilang langsung tampil walau belum diverifikasi`() {
        assertTrue(report(ItemType.LOST).isPublished())
    }

    @Test
    fun `laporan temuan baru tampil setelah keluar dari status dilaporkan`() {
        assertFalse(report(ItemType.FOUND, ReportStatus.DILAPORKAN).isPublished())
        assertTrue(report(ItemType.FOUND, ReportStatus.DISERAHKAN_KE_POS).isPublished())
        assertTrue(report(ItemType.FOUND, ReportStatus.SIAP_DIAMBIL).isPublished())
    }

    @Test
    fun `laporan yang diarsipkan selalu disembunyikan`() {
        assertFalse(report(ItemType.LOST, archived = true).isPublished())
        assertFalse(
            report(ItemType.FOUND, ReportStatus.DISERAHKAN_KE_POS, archived = true).isPublished()
        )
    }
}