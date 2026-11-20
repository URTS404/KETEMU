package org.umn.application_ketemu.domain.model

/**
 * Hasil pencatatan serah terima barang oleh petugas pos.
 *
 * [auditDiServer] bernilai true bila penutupan laporan dikerjakan fungsi
 * `complete_handover` di database (db/0028), karena fungsi itu sudah menulis
 * catatan auditnya sendiri. Bila false — jalur cadangan klien yang dipakai —
 * aplikasi yang harus mencatat auditnya supaya jejaknya tidak hilang dan tidak
 * pula tercatat dua kali.
 */
data class HandoverResult(
    val berhasil: Boolean,
    val auditDiServer: Boolean = false
)
