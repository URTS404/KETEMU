package org.umn.application_ketemu.domain.model

/**
 * Status pengajuan klaim barang.
 *
 * Alur: [MENUNGGU] -> diputuskan STAFF/ADMIN -> [DISETUJUI] atau [DITOLAK].
 * Hanya klaim yang disetujui yang memiliki kode pengambilan.
 */
enum class ClaimStatus {
    MENUNGGU,
    DISETUJUI,
    DITOLAK
}
