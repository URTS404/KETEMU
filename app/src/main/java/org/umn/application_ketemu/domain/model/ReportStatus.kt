package org.umn.application_ketemu.domain.model

/**
 * Alur status barang — inti fitur "Dashboard Tracking Barang" pada PRD:
 *
 *   Dilaporkan -> Diserahkan ke Pos (nama pos) -> Siap Diambil -> Selesai Diklaim
 *
 * [step] dipakai UI untuk menggambar progress timeline, sehingga tidak ada
 * logika urutan status yang tersebar (duplikasi) di layer tampilan.
 *
 * Transisi ke DISERAHKAN_KE_POS, SIAP_DIAMBIL, dan SELESAI_DIKLAIM hanya boleh
 * dilakukan oleh STAFF/ADMIN (ditegakkan juga lewat Row Level Security di Supabase).
 */
enum class ReportStatus(val step: Int) {
    DILAPORKAN(1),
    DISERAHKAN_KE_POS(2),
    SIAP_DIAMBIL(3),
    SELESAI_DIKLAIM(4);

    companion object {
        /** Jumlah total tahap, dipakai timeline agar konsisten di semua layar. */
        const val TOTAL_STEPS: Int = 4
    }
}
