package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportStatus

/**
 * Kontrak akses data laporan.
 *
 * ViewModel hanya bergantung pada interface ini (bukan Room / Supabase langsung),
 * sehingga penerapannya bisa diganti tanpa menyentuh layer tampilan:
 *   M0-UI      : InMemoryReportRepository (data contoh)
 *   M3         : RoomReportRepository (Single Source of Truth, offline-first)
 *   M4         : Room + Supabase + WorkManager (outbox auto-sync)
 *
 * Semua pembacaan berbentuk Flow mengikuti prinsip "Single Source of Truth":
 * UI bereaksi terhadap perubahan data lokal, bukan terhadap response API.
 */
interface ReportRepository {

    /** Seluruh laporan, terbaru di depan. Dipakai Beranda. */
    fun observeReports(): Flow<List<Report>>

    /** Laporan milik satu pengguna, terbaru di depan. Dipakai layar "Laporan Saya". */
    fun observeMyReports(ownerId: String): Flow<List<Report>>

    /** Satu laporan untuk halaman detail (null bila tidak ditemukan). */
    suspend fun getReport(id: String): Report?

    /**
     * Menyimpan laporan baru.
     *
     * Sejak M3 method ini WAJIB menulis ke Room lebih dulu beserta baris antrean
     * (outbox) dalam satu transaksi, sehingga tetap berhasil walau sedang offline.
     */
    suspend fun addReport(report: Report)

    /**
     * Mengubah status barang pada alur tracking.
     *
     * Sejak M3 method ini juga menandai record sebagai PENDING dan menambah baris
     * antrean (outbox), sehingga perubahan status tetap tersimpan walau offline.
     */
    suspend fun updateStatus(reportId: String, status: ReportStatus)
}
