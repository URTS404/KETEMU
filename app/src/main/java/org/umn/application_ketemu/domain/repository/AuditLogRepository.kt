package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.umn.application_ketemu.domain.model.AuditLogEntry

/**
 * Kontrak log aktivitas (audit trail).
 *
 * Append-only: implementasi tidak menyediakan operasi ubah/hapus, sehingga
 * jejak keputusan moderator tidak bisa ditimpa dari layer tampilan.
 */
interface AuditLogRepository {

    /** Seluruh entri, terbaru di depan. */
    fun observeEntries(): Flow<List<AuditLogEntry>>

    suspend fun record(entry: AuditLogEntry)
}
