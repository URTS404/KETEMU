package org.umn.application_ketemu.domain.model

/**
 * Jenis laporan (PRD bagian Pelaporan Barang Hilang / Ditemukan).
 * LOST  = barang hilang (dilaporkan pemiliknya)
 * FOUND = barang ditemukan (dititipkan ke pos satpam)
 */
enum class ItemType {
    LOST,
    FOUND
}
