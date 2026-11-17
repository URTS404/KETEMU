package org.umn.application_ketemu.domain.model

/**
 * Master data 4 pos fisik sesuai PRD (3 pos gedung + basement).
 *
 * Nilai [PosLocation.id] harus sama dengan tabel `pos_locations` di Supabase
 * (lihat `db/0003_seed.sql`) agar `reports.pos_id` yang ditulis panel staf tetap
 * valid. Katalog ini menjadi satu-satunya sumber daftar pos: dipakai data contoh
 * dan dialog serah-terima satpam, sehingga tidak ada daftar pos yang terduplikasi.
 *
 * Pos tujuan TIDAK dipilih pelapor saat membuat laporan; satpam yang
 * mengonfirmasinya saat menerima barang (lihat `AdminModerationScreen`).
 */
object PosCatalog {

    val all: List<PosLocation> = listOf(
        PosLocation(id = "pos-basement", name = "Pos Basement", building = "Basement Gedung Utama"),
        PosLocation(id = "pos-gedung-b", name = "Pos Gedung B", building = "Lobby Gedung B, Lt. 1"),
        PosLocation(id = "pos-gedung-c", name = "Pos Gedung C", building = "Pintu Utama Gedung C"),
        PosLocation(id = "pos-gedung-d", name = "Pos Gedung D", building = "Lobby Gedung D, Lt. 1")
    )

    /** Pos yang dipilih lebih dulu saat satpam belum menentukan tujuan. */
    val default: PosLocation = all.first()

    /** Mencari pos berdasarkan id; `null` bila id tidak dikenal. */
    fun byId(id: String?): PosLocation? = all.firstOrNull { it.id == id }
}
