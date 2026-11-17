package org.umn.application_ketemu.domain.model

/**
 * Peran pengguna sesuai PRD bagian Target Audience (closed-ecosystem kampus).
 *
 * USER  : mahasiswa/dosen — melapor, mencari, mengajukan klaim
 * STAFF : satpam/petugas pos — memverifikasi pengambilan barang (anti-fraud)
 * ADMIN : pengawas sistem — mengelola user & memantau log aktivitas
 *
 * Pemisahan STAFF vs ADMIN disengaja: STAFF bekerja pada **barang** (verifikasi,
 * arsip, serah-terima), ADMIN bekerja pada **sistem** (peran akun, master data,
 * penghapusan permanen).
 */
enum class UserRole {
    USER,
    STAFF,
    ADMIN;

    /**
     * Hak membuka panel moderasi (verifikasi laporan, keputusan klaim, log).
     *
     * Dipakai di dua tempat: menyembunyikan tombol masuk panel admin, dan
     * menolak aksi bila peran berubah saat layar sudah terbuka. Di Supabase
     * aturan yang sama ditegakkan lagi lewat Row Level Security.
     */
    val canModerate: Boolean
        get() = this == STAFF || this == ADMIN

    /** Hak mengelola akun: ubah peran, verifikasi, dan blokir pengguna. */
    val canManageUsers: Boolean
        get() = this == ADMIN

    /** Hak menghapus laporan secara permanen (STAFF hanya boleh mengarsipkan). */
    val canDeleteReports: Boolean
        get() = this == ADMIN
}
