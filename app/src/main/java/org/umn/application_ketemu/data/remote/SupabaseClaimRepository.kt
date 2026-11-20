package org.umn.application_ketemu.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
import org.umn.application_ketemu.domain.model.ReportStatus
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

    /**
     * Mencatat serah terima barang.
     *
     * Jalur utama memakai RPC `complete_handover` (db/0028) supaya seluruh aturan
     * bisnis dan audit berada di database. Bila migrasi itu belum dijalankan,
     * fungsi tersebut tidak ada dan PostgREST membalas galat "Could not find the
     * function"; pada kondisi itu langkah yang sama dikerjakan dari klien memakai
     * hak akses petugas (RLS `reports_update`). Bedanya hanya pencatatan audit di
     * server, sehingga jalur klien ditandai sebagai cadangan, bukan pengganti.
     */
    override suspend fun completeHandover(claimId: String, code: String?): Boolean {
        val kode = code?.trim().orEmpty()
        val galat = runCatching {
            client.postgrest.rpc(
                function = "complete_handover",
                parameters = buildJsonObject {
                    put("p_claim_id", claimId)
                    // Parameter opsional: dikirim hanya bila petugas mengetik kode.
                    if (kode.isNotEmpty()) put("p_code", kode)
                }
            )
        }.exceptionOrNull()

        if (galat == null) {
            refresh()
            return true
        }

        val pesan = galat.message.orEmpty()
        android.util.Log.w("Claims", "complete_handover gagal: $pesan", galat)
        if (!pesan.contains("Could not find the function") &&
            !pesan.contains("PGRST202") &&
            !pesan.contains("404")
        ) {
            // Aturan bisnis menolak (kode salah, klaim belum disetujui, dsb.).
            throw galat
        }

        return serahTerimaDiKlien(claimId, kode)
    }

    /**
     * Cadangan ketika db/0028 belum dijalankan.
     *
     * Urutannya sengaja sama dengan RPC: periksa klaim, cocokkan kode, tutup
     * laporan temuan, tutup juga laporan hilang pasangannya, lalu beri tahu
     * pemiliknya. Kegagalan satu langkah opsional tidak membatalkan yang lain.
     */
    private suspend fun serahTerimaDiKlien(claimId: String, kode: String): Boolean {
        val klaim = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw("id,status,pickup_code,report_id,claimant_id")) {
                filter { eq("id", claimId) }
            }
            .decodeSingleOrNull<HandoverClaimRow>()
            ?: error("klaim tidak ditemukan")

        if (runCatching { ClaimStatus.valueOf(klaim.status) }.getOrNull() != ClaimStatus.DISETUJUI) {
            error("klaim belum disetujui")
        }

        val kodeTersimpan = klaim.pickupCode.orEmpty()
        if (kode.isNotEmpty() && kodeTersimpan.isNotEmpty() &&
            !kode.equals(kodeTersimpan, ignoreCase = true)
        ) {
            error("kode pengambilan tidak cocok")
        }

        client.postgrest.from(REPORT_TABLE).update({
            set("status", ReportStatus.SELESAI_DIKLAIM.name)
        }) {
            filter { eq("id", klaim.reportId) }
        }

        // Laporan hilang milik pengklaim yang berpasangan dengan temuan ini ikut
        // ditutup supaya keduanya selesai pada saat yang sama.
        val laporanHilang = runCatching {
            client.postgrest
                .from("matches")
                .select(columns = Columns.raw("lost_report_id")) {
                    filter { eq("found_report_id", klaim.reportId) }
                }
                .decodeList<HandoverMatchRow>()
                .map { it.lostReportId }
        }.getOrDefault(emptyList())

        laporanHilang.forEach { idHilang ->
            val milikPengklaim = runCatching {
                client.postgrest
                    .from(REPORT_TABLE)
                    .select(columns = Columns.raw("id,owner_id,title,status")) {
                        filter { eq("id", idHilang) }
                    }
                    .decodeSingleOrNull<HandoverReportRow>()
            }.getOrNull()?.takeIf { it.ownerId == klaim.claimantId } ?: return@forEach

            runCatching {
                client.postgrest.from(REPORT_TABLE).update({
                    set("status", ReportStatus.SELESAI_DIKLAIM.name)
                }) {
                    filter { eq("id", milikPengklaim.id) }
                }
            }
        }

        runCatching {
            client.postgrest.from("notifications").insert(
                HandoverNotificationInsert(
                    recipientId = klaim.claimantId,
                    actorId = klaim.claimantId,
                    type = "REPORT_STATUS_CHANGED",
                    title = "Barang sudah diambil",
                    message = "Serah terima barang sudah dicatat petugas. Laporan ditutup.",
                    reportId = klaim.reportId
                )
            )
        }

        refresh()
        return true
    }

    private companion object {
        const val TABLE = "claims"
        const val REPORT_TABLE = "reports"
        const val REFRESH_INTERVAL_MS = 20_000L

        /** `profiles` harus diberi nama constraint karena tabel ini punya dua FK ke sana. */
        const val SELECT_COLUMNS =
            "id,report_id,claimant_id,answer,status,decision_note,pickup_code,created_at,decided_at," +
                
                "reports(id,title,status)," +
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
        pickupCode = pickupCode,
        reportStatus = reports?.status?.let { runCatching { ReportStatus.valueOf(it) }.getOrNull() },
        handedOverAt = handedOverAt?.toEpochMillisOrNull()
    )
}

private fun String.toEpochMillisOrNull(): Long? =
    runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }.getOrNull()

/** Baris minimal yang dibaca saat mencatat serah terima di sisi klien. */
@kotlinx.serialization.Serializable
private data class HandoverClaimRow(
    val id: String = "",
    val status: String = "",
    @kotlinx.serialization.SerialName("pickup_code") val pickupCode: String? = null,
    @kotlinx.serialization.SerialName("report_id") val reportId: String = "",
    @kotlinx.serialization.SerialName("claimant_id") val claimantId: String = ""
)


/** Laporan hilang milik pengklaim yang ikut ditutup. */
@kotlinx.serialization.Serializable
private data class HandoverReportRow(
    val id: String = "",
    @kotlinx.serialization.SerialName("owner_id") val ownerId: String = "",
    val title: String = "",
    val status: String = ""
)

/** Notifikasi penutup alur untuk pemilik barang. */
@kotlinx.serialization.Serializable
private data class HandoverNotificationInsert(
    @kotlinx.serialization.SerialName("recipient_id") val recipientId: String,
    @kotlinx.serialization.SerialName("actor_id") val actorId: String,
    val type: String,
    val title: String,
    val message: String,
    @kotlinx.serialization.SerialName("report_id") val reportId: String
)


/** Pasangan cocok minimal yang dibaca saat menutup laporan hilang. */
@kotlinx.serialization.Serializable
private data class HandoverMatchRow(
    @kotlinx.serialization.SerialName("lost_report_id") val lostReportId: String = ""
)
