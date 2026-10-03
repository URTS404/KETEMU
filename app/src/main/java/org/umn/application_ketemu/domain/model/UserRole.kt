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
    ADMIN
}
