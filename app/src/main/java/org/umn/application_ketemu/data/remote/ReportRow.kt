package org.umn.application_ketemu.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO PostgREST untuk tabel `reports`.
 *
 * Sengaja terpisah dari [org.umn.application_ketemu.domain.model.Report]: layer
 * domain tidak boleh bergantung pada bentuk response API (Single Source of Truth,
 * PRD). Pemetaan dilakukan di [SupabaseReportRepository].
 */
@Serializable
internal data class ReportRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val type: String = "LOST",
    val title: String = "",
    val description: String = "",
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("last_seen_location") val lastSeenLocation: String = "",
    /** Titik GPS opsional; null untuk laporan lama atau tanpa penanda peta. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "DILAPORKAN",
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("pos_locations") val posLocation: PosRow? = null,
    val profiles: ProfileRow? = null
)

@Serializable
internal data class PosRow(
    val id: String = "",
    val name: String = "",
    val building: String = ""
)

@Serializable
internal data class ProfileRow(
    @SerialName("full_name") val fullName: String = ""
)
