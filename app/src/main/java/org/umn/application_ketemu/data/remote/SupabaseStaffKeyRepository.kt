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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.umn.application_ketemu.domain.model.StaffKey
import org.umn.application_ketemu.domain.repository.StaffKeyRepository
import java.time.OffsetDateTime

/**
 * [StaffKeyRepository] di atas tabel `staff_keys` + RPC di `db/0015`.
 *
 * Penulisan sengaja tidak memakai INSERT/UPDATE langsung: tabel kunci tidak
 * punya policy tulis, sehingga satu-satunya jalan membuat atau menebus kunci
 * adalah fungsi SECURITY DEFINER di server.
 */
class SupabaseStaffKeyRepository(
    private val client: SupabaseClient
) : StaffKeyRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = MutableStateFlow<List<StaffKey>>(emptyList())

    init {
        scope.launch {
            while (isActive) {
                runCatching { load() }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun observeKeys(): StateFlow<List<StaffKey>> = cache.asStateFlow()

    override suspend fun refresh() {
        load()
    }

    override suspend fun regenerate(): Boolean = runCatching {
        client.postgrest.rpc("generate_staff_keys", buildJsonObject { put("p_count", 10) })
        load()
        true
    }.getOrElse {
        android.util.Log.e("StaffKeys", "generate gagal: ${it.message}", it)
        false
    }

    override suspend fun redeem(code: String, targetUserId: String?): Boolean = runCatching {
        val args = buildJsonObject {
            put("p_code", code.trim().uppercase())
            if (targetUserId != null) put("p_target", targetUserId)
        }
        val ok = client.postgrest.rpc("redeem_staff_key", args).decodeAs<Boolean>()
        load()
        ok
    }.getOrElse {
        android.util.Log.e("StaffKeys", "redeem gagal: ${it.message}", it)
        false
    }

    private suspend fun load() {
        cache.value = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(SELECT_COLUMNS)) {
                order("created_at", Order.DESCENDING)
                limit(20)
            }
            .decodeList<StaffKeyDto>()
            .map { it.toModel() }
    }

    private fun StaffKeyDto.toModel() = StaffKey(
        code = code,
        reservedEmail = reservedEmail,
        note = note,
        usedBy = usedBy,
        usedAt = usedAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() },
        createdAt = createdAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0L
    )

    @Serializable
    private data class StaffKeyDto(
        val code: String = "",
        @SerialName("reserved_email") val reservedEmail: String? = null,
        val note: String? = null,
        @SerialName("used_by") val usedBy: String? = null,
        @SerialName("used_at") val usedAt: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    private companion object {
        const val TABLE = "staff_keys"
        const val REFRESH_INTERVAL_MS = 30_000L
        const val SELECT_COLUMNS =
            "code,reserved_email,note,used_by,used_at,created_at"
    }
}
