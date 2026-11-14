package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.umn.application_ketemu.domain.model.AuditLogEntry
import org.umn.application_ketemu.domain.repository.AuditLogRepository

/**
 * Log aktivitas di memori.
 *
 * Append-only dan dibatasi [MAX_ENTRIES] entri terbaru agar pemakaian memori
 * pada demo tetap wajar. Urutan yang disajikan selalu terbaru di depan.
 */
class InMemoryAuditLogRepository(
    initialEntries: List<AuditLogEntry> = emptyList()
) : AuditLogRepository {

    private val entries = MutableStateFlow(initialEntries)

    override fun observeEntries(): Flow<List<AuditLogEntry>> =
        entries.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun record(entry: AuditLogEntry) {
        entries.value = (entries.value + entry)
            .sortedByDescending { it.createdAt }
            .take(MAX_ENTRIES)
    }

    companion object {
        const val MAX_ENTRIES = 200
    }
}
