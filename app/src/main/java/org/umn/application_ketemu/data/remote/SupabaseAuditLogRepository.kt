package org.umn.application_ketemu.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.umn.application_ketemu.domain.model.AuditAction
import org.umn.application_ketemu.domain.model.AuditLogEntry
import org.umn.application_ketemu.domain.repository.AuditLogRepository
import java.time.OffsetDateTime

/**
 * Log aktivitas (append-only) di atas tabel `audit_logs`.
 *
 * Sebagian baris dibuat otomatis oleh trigger database (mis. perubahan status
 * laporan), sebagian lagi oleh aksi moderator di aplikasi lewat [record].
 * Tidak ada operasi ubah/hapus — sesuai kebijakan RLS yang hanya mengizinkan
 * SELECT dan INSERT.
 */
class SupabaseAuditLogRepository(
    private val client: SupabaseClient
) : AuditLogRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = MutableStateFlow<List<AuditLogEntry>>(emptyList())

    init {
        scope.launch {
            while (isActive) {
                runCatching { refresh() }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    suspend fun refresh() {
        val rows = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw("id,actor_id,actor_name,action,target_type,target_id,note,created_at")) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<AuditRow>()
        // Satu aksi bisa tercatat dua kali: trigger database (selalu) dan layer
        // ViewModel (agar mode in-memory juga punya jejak). Keduanya peristiwa
        // yang sama, jadi pembaca menggabungkannya dalam jendela 10 detik.
        cache.value = rows.mapNotNull { it.toDomain() }
            .distinctBy { entry ->
                "${entry.action}|${entry.targetType}|${entry.targetId}|${entry.actorId}|${entry.createdAt / DEDUPE_WINDOW_MS}"
            }
    }

    override fun observeEntries(): Flow<List<AuditLogEntry>> =
        cache.asStateFlow().onStart { runCatching { refresh() } }

    override suspend fun record(entry: AuditLogEntry) {
        client.postgrest.from(TABLE).insert(
            AuditInsert(
                actorId = entry.actorId.takeIf { it.isNotBlank() },
                actorName = entry.actorName,
                action = entry.action.name,
                targetType = entry.targetType,
                targetId = entry.targetId.takeIf { it.isNotBlank() },
                note = entry.note
            )
        )
        runCatching { refresh() }
    }

    private companion object {
        const val TABLE = "audit_logs"
        const val REFRESH_INTERVAL_MS = 20_000L
        const val DEDUPE_WINDOW_MS = 10_000L
    }
}

internal fun AuditRow.toDomain(): AuditLogEntry? {
    val parsedCreatedAt = createdAt?.let {
        runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    } ?: return null
    return AuditLogEntry(
        id = id,
        actorId = actorId.orEmpty(),
        actorName = actorName.ifBlank { "Sistem" },
        action = runCatching { AuditAction.valueOf(action) }
            .getOrDefault(AuditAction.VERIFIKASI_LAPORAN),
        targetType = targetType,
        targetId = targetId.orEmpty(),
        note = note,
        createdAt = parsedCreatedAt
    )
}
