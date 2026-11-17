package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportMatch

/**
 * Perilaku [InMemoryMatchRepository] yang dipakai halaman detail:
 * penyaringan per laporan, urutan terbaru, dan pemilihan sisi kandidat.
 */
class InMemoryMatchRepositoryTest {

    private fun report(
        id: String,
        type: ItemType,
        title: String = "Barang $id"
    ) = Report(
        id = id,
        ownerId = "user-$id",
        ownerName = "Pengguna $id",
        type = type,
        title = title,
        description = "",
        category = ReportCategory.AKSESORIS,
        lastSeenLocation = "Kampus",
        createdAt = 1_700_000_000_000L
    )

    private fun match(id: String, lost: Report, found: Report, matchedAt: Long) =
        ReportMatch(id = id, lostReport = lost, foundReport = found, matchedAt = matchedAt)

    @Test
    fun `hanya pasangan yang melibatkan laporan diminta yang dikembalikan`() = runTest {
        val lostA = report("rpt-a", ItemType.LOST)
        val foundA = report("rpt-b", ItemType.FOUND)
        val lostB = report("rpt-c", ItemType.LOST)
        val repository = InMemoryMatchRepository(
            listOf(
                match("m-1", lostA, foundA, matchedAt = 1_000L),
                match("m-2", lostB, foundA, matchedAt = 2_000L)
            )
        )

        val result = repository.observeMatchesFor("rpt-b").first()

        // Kedua pasangan menyangkut rpt-b, terbaru lebih dahulu.
        assertEquals(listOf("m-2", "m-1"), result.map { it.id })
    }

    @Test
    fun `pasangan terbaru muncul lebih dahulu`() = runTest {
        val lost = report("rpt-lost", ItemType.LOST)
        val found = report("rpt-found", ItemType.FOUND)
        val repository = InMemoryMatchRepository(
            listOf(
                match("m-lama", lost, found, matchedAt = 1_000L),
                match("m-baru", lost, found, matchedAt = 5_000L)
            )
        )

        val result = repository.observeMatchesFor("rpt-lost").first()

        assertEquals(listOf("m-baru", "m-lama"), result.map { it.id })
    }

    @Test
    fun `candidateFor mengembalikan laporan sisi seberang dari dua arah`() {
        val lost = report("rpt-lost", ItemType.LOST)
        val found = report("rpt-found", ItemType.FOUND)
        val match = match("m-1", lost, found, matchedAt = 1_000L)

        assertEquals(found, match.candidateFor("rpt-lost"))
        assertEquals(lost, match.candidateFor("rpt-found"))
        assertNull(match.candidateFor("rpt-lain"))
        assertEquals("rpt-lost", match.lostReportId)
        assertEquals("rpt-found", match.foundReportId)
    }

    @Test
    fun `laporan tanpa pasangan menghasilkan daftar kosong`() = runTest {
        val repository = InMemoryMatchRepository(
            listOf(
                match(
                    id = "m-1",
                    lost = report("rpt-lost", ItemType.LOST),
                    found = report("rpt-found", ItemType.FOUND),
                    matchedAt = 1_000L
                )
            )
        )

        val result = repository.observeMatchesFor("rpt-tidak-ada").first()

        assertTrue(result.isEmpty())
    }
}
