package org.umn.application_ketemu.domain.model

/**
 * Pasangan laporan hilang & temuan hasil pencocokan server (db/0018).
 *
 * Model menyimpan KEDUA laporan, bukan hanya sisi "kandidat", karena satu baris
 * `matches` dibaca dari dua arah: pemilik laporan hilang melihat barang
 * temuannya, sedangkan penemu melihat laporan kehilangan yang cocok. Sisi yang
 * ditampilkan dipilih lewat [candidateFor] sesuai laporan yang sedang dibuka.
 *
 * @param matchedAt epoch millis; label waktu relatif dibuat di layer UI.
 */
data class ReportMatch(
    val id: String,
    val lostReport: Report,
    val foundReport: Report,
    val matchedAt: Long
) {

    val lostReportId: String get() = lostReport.id

    val foundReportId: String get() = foundReport.id

    /**
     * Laporan di sisi seberang untuk pengamat [reportId], atau `null` bila
     * pasangan ini tidak melibatkan laporan tersebut.
     */
    fun candidateFor(reportId: String): Report? = when (reportId) {
        lostReport.id -> foundReport
        foundReport.id -> lostReport
        else -> null
    }
}
