package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.umn.application_ketemu.domain.model.AuditAction
import org.umn.application_ketemu.domain.model.AuditLogEntry

class InMemoryAuditLogRepositoryTest {

    private fun entry(id: String, createdAt: Long) = AuditLogEntry(
        id = id,
        actorId = "user-staff-01",
        actorName = "Pak Rahmat (Satpam)",
        action = AuditAction.VERIFIKASI_LAPORAN,
        targetType = "REPORT",
        targetId = "rpt-001",
        note = "Tumbler",
        createdAt = createdAt
    )

    @Test
    fun `entri terbaru tampil lebih dulu`() = runTest {
        val repository = InMemoryAuditLogRepository()
        repository.record(entry("a", 1_000L))
        repository.record(entry("b", 2_000L))

        val entries = repository.observeEntries().first()

        assertEquals(listOf("b", "a"), entries.map { it.id })
    }

    @Test
    fun `isi log dibatasi jumlah maksimum`() = runTest {
        val repository = InMemoryAuditLogRepository()

        repeat(InMemoryAuditLogRepository.MAX_ENTRIES + 5) { index ->
            repository.record(entry("e-$index", index.toLong()))
        }

        val entries = repository.observeEntries().first()

        assertEquals(InMemoryAuditLogRepository.MAX_ENTRIES, entries.size)
        assertEquals("e-${InMemoryAuditLogRepository.MAX_ENTRIES + 4}", entries.first().id)
    }
}
