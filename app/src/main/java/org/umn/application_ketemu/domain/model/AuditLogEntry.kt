package org.umn.application_ketemu.domain.model

/** Jenis aksi yang dicatat pada log aktivitas moderator. */
enum class AuditAction {
    VERIFIKASI_LAPORAN,
    ARSIP_LAPORAN,
    BUKA_ARSIP_LAPORAN,
    SETUJUI_KLAIM,
    TOLAK_KLAIM
}

/**
 * Satu baris log aktivitas (audit trail).
 *
 * Audit log tidak pernah diubah/dihapus: setiap aksi moderator menambah baris
 * baru, sehingga riwayat keputusan bisa dipertanggungjawabkan saat evaluasi.
 *
 * @param targetType "REPORT" atau "CLAIM" (memudahkan pemetaan ke tabel Supabase).
 */
data class AuditLogEntry(
    val id: String,
    val actorId: String,
    val actorName: String,
    val action: AuditAction,
    val targetType: String,
    val targetId: String,
    val note: String? = null,
    val createdAt: Long
)
