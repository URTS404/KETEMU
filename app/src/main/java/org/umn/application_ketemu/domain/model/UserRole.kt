package org.umn.application_ketemu.domain.model

/**
 * Peran pengguna sesuai PRD bagian Target Audience (closed-ecosystem kampus).
 *
 * USER  : mahasiswa/dosen — melapor, mencari, mengajukan klaim
 * STAFF : satpam/petugas pos — memverifikasi pengambilan barang (anti-fraud)
 * ADMIN : pengawas sistem — mengelola user & memantau log aktivitas
 */
enum class UserRole {
    USER,
    STAFF,
    ADMIN;

    /**
     * Hak membuka panel moderasi.
     *
     * Dipakai di dua tempat: menyembunyikan tombol masuk panel admin, dan
     * menolak aksi bila peran berubah saat layar sudah terbuka. Di Supabase
     * nanti aturan yang sama ditegakkan lagi lewat Row Level Security.
     */
    val canModerate: Boolean
        get() = this == STAFF || this == ADMIN
}
