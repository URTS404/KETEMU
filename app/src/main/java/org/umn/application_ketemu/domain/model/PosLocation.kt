package org.umn.application_ketemu.domain.model

/**
 * Pos titipan barang (master data 4 pos fisik sesuai PRD: 4 pos gedung + basement).
 *
 * [name] dipakai pada timeline status ("Diserahkan ke Pos (nama pos)") sehingga
 * status barang tidak hanya berisi id, tetapi bisa langsung dibaca pengguna.
 */
data class PosLocation(
    val id: String,
    val name: String,
    val building: String,
    val isActive: Boolean = true
)
