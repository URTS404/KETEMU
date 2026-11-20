package org.umn.application_ketemu.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.umn.application_ketemu.domain.model.GeoPoint
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.PosLocation
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.SyncStatus

/**
 * Salinan lokal satu laporan ([Report]) di tabel `reports_cache`.
 *
 * Kenapa seluruh kolom diduplikasi (bukan hanya id lalu ambil dari server):
 * Beranda dan Laporan Saya harus tetap bisa dibaca saat perangkat offline.
 * Room menjadi **satu-satunya sumber data yang dibaca UI**; Supabase hanya
 * mengisi tabel ini.
 *
 * [syncStatus] menyimpan keadaan sinkronisasi, bukan [Report.syncStatus] yang
 * dihitung saat pemetaan — baris PENDING berarti masih ada antrean di tabel
 * `outbox` untuk laporan ini dan nilainya tidak boleh ditimpa hasil server.
 */
@Entity(
    tableName = "reports_cache",
    indices = [Index("ownerId"), Index("status"), Index("syncStatus")]
)
data class ReportEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val ownerName: String,
    val type: String,
    val title: String,
    val description: String,
    val categoryId: String,
    val photoUri: String?,
    val lastSeenLocation: String,
    val posId: String?,
    val posName: String?,
    val posBuilding: String?,
    val status: String,
    val syncStatus: String,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
    val latitude: Double?,
    val longitude: Double?
)

/** Pemetaan baris lokal -> model domain yang dipakai UI. */
fun ReportEntity.toDomain(): Report = Report(
    id = id,
    ownerId = ownerId,
    ownerName = ownerName,
    type = runCatching { ItemType.valueOf(type) }.getOrDefault(ItemType.LOST),
    title = title,
    description = description,
    category = runCatching { ReportCategory.valueOf(categoryId) }
        .getOrDefault(ReportCategory.LAINNYA),
    photoUri = photoUri,
    lastSeenLocation = lastSeenLocation,
    pos = posId?.let { PosLocation(id = it, name = posName.orEmpty(), building = posBuilding.orEmpty()) },
    status = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.DILAPORKAN),
    syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.PENDING),
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
    coordinates = latitude?.let { lat ->
        longitude?.let { lng -> GeoPoint(latitude = lat, longitude = lng).takeIf { it.isValid } }
    }
)

/** Pemetaan model domain -> baris lokal. */
fun Report.toEntity(syncStatus: SyncStatus): ReportEntity = ReportEntity(
    id = id,
    ownerId = ownerId,
    ownerName = ownerName,
    type = type.name,
    title = title,
    description = description,
    categoryId = category.name,
    photoUri = photoUri,
    lastSeenLocation = lastSeenLocation,
    posId = pos?.id,
    posName = pos?.name,
    posBuilding = pos?.building,
    status = status.name,
    syncStatus = syncStatus.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
    latitude = coordinates?.latitude,
    longitude = coordinates?.longitude
)
