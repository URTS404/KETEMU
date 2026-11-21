package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.umn.application_ketemu.domain.model.HasilCariFoto
import org.umn.application_ketemu.domain.model.PosLocation
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

    /** Laporan yang disembunyikan moderator (tidak tampil di Beranda). */
    fun observeArchivedReports(): Flow<List<Report>>

    /**
     * Antrean moderasi: laporan yang belum diverifikasi petugas.
     *
     * Urut terlama dulu (FIFO) karena antrean dilayani dari yang paling lama
     * menunggu. Laporan yang sudah diarsipkan tidak ikut.
     */
    fun observeModerationQueue(): Flow<List<Report>>

    /** Menyembunyikan / memunculkan laporan dari daftar publik (khusus moderator). */
    suspend fun setArchived(reportId: String, archived: Boolean)

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

    /**
     * Menyimpan hasil serah-terima fisik barang ke pos.
     *
     * Status berpindah ke [ReportStatus.DISERAHKAN_KE_POS] sekaligus mencatat
     * [pos] tujuan yang dikonfirmasi satpam. Pos sengaja TIDAK dipilih pelapor
     * saat membuat laporan: hanya petugas yang tahu barang benar-benar
     * dititipkan di pos mana.
     *
     * Dipisah dari [updateStatus] agar pemanggil lama (klaim, dsb.) tidak perlu
     * menyertakan pos dan tetap kompatibel.
     */
    suspend fun confirmHandover(reportId: String, pos: PosLocation)

    /**
     * Menghapus laporan milik pengguna sendiri.
     *
     * Dipakai saat pengguna salah kirim laporan, atau barang hilangnya sudah
     * ditemukan sehingga laporannya tidak perlu dicari lagi. Server menolak
     * penghapusan bila laporan masih punya klaim aktif (`MENUNGGU`/`DISETUJUI`)
     * — kebijakan RLS `reports_delete_own` (db/0031).
     *
     * Berbeda dengan moderasi yang memakai [setArchived]: menghapus benar-benar
     * membuang baris, jadi tidak ada jejak yang bisa dipulihkan petugas.
     * Implementasi boleh melempar [IllegalStateException] bila memerlukan
     * jaringan dan perangkat sedang offline.
     */
    /**
     * Nama seluruh Pos yang terdaftar di server (tabel pos_locations).
     *
     * Dipakai filter "Dititipkan di Pos" agar pilihannya lengkap, bukan hanya Pos
     * yang kebetulan sudah punya laporan. Implementasi tanpa jaringan boleh
     * mengembalikan daftar kosong: pemanggil tetap menggabungkannya dengan nama
     * Pos yang diambil dari laporan.
     */
    suspend fun daftarNamaPos(): List<String> = emptyList()

    suspend fun hapusLaporan(reportId: String)

    /**
     * Menyimpan sidik rupa foto laporan (db/0032) agar bisa ditemukan lewat
     * pencarian "Lens".
     *
     * Vektor dihitung di dalam HP ([org.umn.application_ketemu.core.visual.PencariVisual]),
     * bukan dikirim ke layanan luar. Bila perangkat offline atau server menolak,
     * metode ini cukup mengembalikan `false` — laporan tetap sah, hanya tidak
     * ikut serta dalam pencarian berbasis foto.
     */
    suspend fun simpanVektorFoto(reportId: String, vektor: FloatArray): Boolean = false

    /**
     * Mencari laporan temuan yang fotonya mirip dengan [vektor] (fitur "Lens").
     *
     * [vektor] harus 1024 angka ternormalisasi L2 dari [org.umn.application_ketemu.core.visual.PencariVisual].
     * Hasil diurutkan dari yang paling mirip; daftar kosong berarti belum ada
     * laporan temuan berfoto yang cukup mirip (atau perangkat sedang offline).
     * Versi bawaan mengembalikan daftar kosong supaya implementasi demo tetap
     * bisa dipakai tanpa database vektor.
     */
    suspend fun cariDenganFoto(vektor: FloatArray, batas: Int = 20): List<HasilCariFoto> =
        emptyList()
}
