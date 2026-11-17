package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.umn.application_ketemu.domain.model.ReportMatch

/**
 * Kontrak pencocokan otomatis laporan hilang <-> temuan.
 *
 * Pencocokan DIHITUNG DI SERVER (trigger `reports_auto_match`, db/0018) supaya
 * berlaku untuk semua pengguna, termasuk yang aplikasinya sedang tertutup.
 * Klien hanya membaca hasilnya; karena itu tidak ada method tulis di sini dan
 * tabel `matches` pun tidak punya policy INSERT/UPDATE/DELETE.
 */
interface MatchRepository {

    /**
     * Pasangan yang melibatkan [reportId], terbaru lebih dahulu.
     *
     * Dipakai halaman detail untuk bagian "Mungkin barangmu". Penerapan
     * menyaring pasangan yang laporan seberangnya tidak boleh dibaca pengguna
     * (mis. diarsipkan moderator), sehingga daftar kandidat tidak pernah
     * menampilkan tautan mati.
     */
    fun observeMatchesFor(reportId: String): Flow<List<ReportMatch>>
}
