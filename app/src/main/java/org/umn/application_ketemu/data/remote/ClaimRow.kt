package org.umn.application_ketemu.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Baris `claims` beserta relasi yang dibutuhkan layar Verifikasi klaim. */
@Serializable
internal data class ClaimRow(
    val id: String,
    @SerialName("report_id") val reportId: String,
    @SerialName("claimant_id") val claimantId: String,
    val answer: String = "",
    val status: String = "MENUNGGU",
    @SerialName("decision_note") val decisionNote: String? = null,
    @SerialName("pickup_code") val pickupCode: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("decided_at") val decidedAt: String? = null,
    @SerialName("handed_over_at") val handedOverAt: String? = null,
    val reports: ClaimReportRow? = null,
    val profiles: ClaimProfileRow? = null
)

@Serializable
internal data class ClaimReportRow(
    val id: String = "",
    val title: String = "",
    val status: String = "DILAPORKAN"
)

@Serializable
internal data class ClaimProfileRow(
    @SerialName("full_name") val fullName: String = "",
    val email: String = ""
)

/** Baris `audit_logs` (append-only, ditulis trigger database + aksi moderator). */
@Serializable
internal data class AuditRow(
    val id: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("actor_name") val actorName: String = "",
    val action: String = "VERIFIKASI_LAPORAN",
    @SerialName("target_type") val targetType: String = "REPORT",
    @SerialName("target_id") val targetId: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/** Payload INSERT untuk klaim baru. */
@Serializable
internal data class ClaimInsert(
    @SerialName("report_id") val reportId: String,
    @SerialName("claimant_id") val claimantId: String,
    val answer: String
)

/** Payload INSERT untuk log aktivitas yang ditulis dari aplikasi. */
@Serializable
internal data class AuditInsert(
    @SerialName("actor_id") val actorId: String?,
    @SerialName("actor_name") val actorName: String,
    val action: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String?,
    val note: String?
)
