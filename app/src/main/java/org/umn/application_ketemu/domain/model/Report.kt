package org.umn.application_ketemu.domain.model

/**
 * Model utama yang dibaca seluruh layar (Beranda, Laporan Saya, Detail, Staff).
 *
 * Catatan arsitektur (Single Source of Truth sesuai catatan evaluator teknis PRD):
 * model ini nantinya dipetakan dari tabel Room. UI TIDAK pernah memakai objek
 * hasil response Supabase secara langsung.
 *
 * @param id dibuat di sisi klien (UUID) supaya unggahan ulang bersifat idempoten
 *           — laporan offline yang dikirim dua kali tidak akan terduplikasi.
 * @param createdAt epoch millis (perhitungan waktu relatif ada di core/util).
 */
data class Report(
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val type: ItemType,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val photoUri: String? = null,
    val lastSeenLocation: String,
    val pos: PosLocation? = null,
    val status: ReportStatus = ReportStatus.DILAPORKAN,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long,
    val updatedAt: Long = createdAt
)
