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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.ClaimStatus
import org.umn.application_ketemu.domain.repository.ClaimRepository
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Klaim barang di atas tabel `claims`.
 *
 * Keputusan klaim TIDAK menghitung apa pun di klien: trigger `decide_claim`
 * membuat `pickup_code`, dan trigger `apply_claim_to_report` memindahkan status
 * laporan menjadi SIAP_DIAMBIL. Aplikasi hanya menulis niat moderator, sehingga
 * aturan bisnis tetap tunggal di database.
 */
class SupabaseClaimRepository(
    private val client: SupabaseClient
) : ClaimRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = MutableStateFlow<List<Claim>>(emptyList())

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
            .select(columns = Columns.raw(SELECT_COLUMNS)) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ClaimRow>()
        cache.value = rows.mapNotNull { it.toDomain() }
    }

    override fun observeClaims(): Flow<List<Claim>> =
        cache.asStateFlow().onStart { runCatching { refresh() } }

    override fun observePendingClaims(): Flow<List<Claim>> =
        cache.asStateFlow()
            .map { claims -> claims.filter { it.status == ClaimStatus.MENUNGGU }.sortedBy { it.createdAt } }
            .onStart { runCatching { refresh() } }

    override suspend fun getClaim(id: String): Claim? =
        client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(SELECT_COLUMNS)) { filter { eq("id", id) } }
            .decodeSingleOrNull<ClaimRow>()
            ?.toDomain()

    override suspend fun submit(claim: Claim) {
        client.postgrest.from(TABLE).insert(
            ClaimInsert(
                reportId = claim.reportId,
                claimantId = claim.claimantId,
                answer = claim.answer
            )
        )
        refresh()
    }

    override suspend fun decide(
        claimId: String,
        approve: Boolean,
        decidedBy: String,
        note: String?
    ): Claim? {
        // `decided_by` bertipe uuid; parameter domain berisi nama moderator, jadi
        // id-nya dicari dulu. Bila tidak ketemu, kolom dibiarkan null (aksi tetap
        // tercatat di audit_logs beserta nama pelakunya).
        val moderatorId = runCatching {
            client.postgrest
                .from("profiles")
                .select(columns = Columns.raw("id")) { filter { eq("full_name", decidedBy) } }
                .decodeSingleOrNull<ModeratorRow>()
                ?.id
        }.getOrNull()

        val decidedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        client.postgrest.from(TABLE).update({
            set("status", if (approve) ClaimStatus.DISETUJUI.name else ClaimStatus.DITOLAK.name)
            set("decided_by", moderatorId)
            set("decided_at", decidedAt)
            set("decision_note", note)
        }) {
            filter { eq("id", claimId) }
        }
        refresh()
        return getClaim(claimId)
    }

    private companion object {
        const val TABLE = "claims"
        const val REFRESH_INTERVAL_MS = 20_000L

        /** `profiles` harus diberi nama constraint karena tabel ini punya dua FK ke sana. */
        const val SELECT_COLUMNS =
            "id,report_id,claimant_id,answer,status,decision_note,pickup_code,created_at,decided_at," +
                "reports(title)," +
                "profiles!claims_claimant_id_fkey(full_name,email)"
    }
}

@kotlinx.serialization.Serializable
private data class ModeratorRow(val id: String = "")

internal fun ClaimRow.toDomain(): Claim? {
    val parsedCreatedAt = createdAt?.toEpochMillisOrNull() ?: return null
    return Claim(
        id = id,
        reportId = reportId,
        reportTitle = reports?.title.orEmpty(),
        claimantId = claimantId,
        claimantName = profiles?.fullName ?: "Pengguna Ketemu",
        claimantEmail = profiles?.email.orEmpty(),
        answer = answer,
        status = runCatching { ClaimStatus.valueOf(status) }.getOrDefault(ClaimStatus.MENUNGGU),
        createdAt = parsedCreatedAt,
        decidedAt = decidedAt?.toEpochMillisOrNull(),
        decidedBy = null,
        decisionNote = decisionNote,
        pickupCode = pickupCode
    )
}

private fun String.toEpochMillisOrNull(): Long? =
    runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }.getOrNull()
