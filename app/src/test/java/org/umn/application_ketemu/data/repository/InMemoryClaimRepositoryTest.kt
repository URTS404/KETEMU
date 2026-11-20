package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.ClaimStatus

class InMemoryClaimRepositoryTest {

    private fun claim(id: String, status: ClaimStatus = ClaimStatus.MENUNGGU) = Claim(
        id = id,
        reportId = "rpt-002",
        reportTitle = "KTM atas nama Andi Wijaya",
        claimantId = "user-lain-03",
        claimantName = "Andi Wijaya",
        claimantEmail = "andi.wijaya@student.umn.ac.id",
        answer = "NIM 00000077777",
        status = status,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun `menyetujui klaim mengisi kode pengambilan dan status disetujui`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-001")))

        val decided = repository.decide("clm-001", approve = true, decidedBy = "Pak Rahmat")

        assertNotNull(decided)
        assertEquals(ClaimStatus.DISETUJUI, decided?.status)
        assertEquals(6, decided?.pickupCode?.length)
        assertEquals("Pak Rahmat", decided?.decidedBy)
        assertNotNull(decided?.decidedAt)
    }

    @Test
    fun `menolak klaim tidak memberi kode pengambilan`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-002")))

        val decided = repository.decide("clm-002", approve = false, decidedBy = "Pak Rahmat", note = "Bukti kurang")

        assertEquals(ClaimStatus.DITOLAK, decided?.status)
        assertNull(decided?.pickupCode)
        assertEquals("Bukti kurang", decided?.decisionNote)
    }

    @Test
    fun `keputusan bersifat idempoten`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-003")))

        val first = repository.decide("clm-003", approve = true, decidedBy = "Pak Rahmat")
        val second = repository.decide("clm-003", approve = false, decidedBy = "Admin Lain")

        assertEquals(first, second)
        assertEquals(ClaimStatus.DISETUJUI, second?.status)
    }

    @Test
    fun `antrean hanya berisi klaim menunggu`() = runTest {
        val repository = InMemoryClaimRepository(
            listOf(
                claim("clm-001"),
                claim("clm-002", status = ClaimStatus.DISETUJUI)
            )
        )

        val pending = repository.observePendingClaims().first()

        assertEquals(1, pending.size)
        assertTrue(pending.all { it.status == ClaimStatus.MENUNGGU })
    }

    @Test
    fun `klaim tak dikenal mengembalikan null`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-001")))

        assertNull(repository.decide("clm-tidak-ada", approve = true, decidedBy = "Pak Rahmat"))
    }

    private suspend fun approved(inMemory: InMemoryClaimRepository, id: String): String {
        inMemory.decide(id, approve = true, decidedBy = "Pak Rahmat")
        return inMemory.getClaim(id)?.pickupCode.orEmpty()
    }

    @Test
    fun `serah terima dengan kode benar menandai waktu pengambilan`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-010")))
        val kode = approved(repository, "clm-010")

        val hasil = repository.completeHandover("clm-010", kode.lowercase())

        assertTrue(hasil.berhasil)
        // Demo tidak punya fungsi database, jadi audit dicatat pemanggil.
        assertTrue(!hasil.auditDiServer)
        assertNotNull(repository.getClaim("clm-010")?.handedOverAt)
    }

    @Test
    fun `serah terima tanpa kode diterima untuk verifikasi KTM manual`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-011")))
        approved(repository, "clm-011")

        val hasil = repository.completeHandover("clm-011", null)

        assertTrue(hasil.berhasil)
        assertNotNull(repository.getClaim("clm-011")?.handedOverAt)
    }

    @Test
    fun `kode salah ditolak dan klaim tidak berubah`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-012")))
        approved(repository, "clm-012")

        val hasil = repository.completeHandover("clm-012", "SALAH1")

        assertTrue(!hasil.berhasil)
        assertNull(repository.getClaim("clm-012")?.handedOverAt)
    }

    @Test
    fun `serah terima hanya untuk klaim yang sudah disetujui`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-013")))

        val hasil = repository.completeHandover("clm-013", null)

        assertTrue(!hasil.berhasil)
        assertNull(repository.getClaim("clm-013")?.handedOverAt)
    }

    @Test
    fun `serah terima klaim tak dikenal gagal`() = runTest {
        val repository = InMemoryClaimRepository(listOf(claim("clm-014")))

        assertTrue(!repository.completeHandover("clm-tidak-ada", null).berhasil)
    }
}
