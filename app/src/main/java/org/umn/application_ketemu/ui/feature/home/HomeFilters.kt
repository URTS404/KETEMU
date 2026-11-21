package org.umn.application_ketemu.ui.feature.home

import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportStatus

/**
 * Rentang waktu laporan di Beranda.
 *
 * Dihitung dari waktu sekarang, bukan tanggal kalender, supaya "24 jam" tetap
 * berarti 24 jam terakhir berapa pun jam pengguna membuka aplikasi.
 */
enum class RentangWaktu(val millis: Long?) {
    SEMUA(null),
    HARI_INI(24L * 60 * 60 * 1000),
    TUJUH_HARI(7L * 24 * 60 * 60 * 1000),
    TIGA_PULUH_HARI(30L * 24 * 60 * 60 * 1000)
}

/** Urutan daftar laporan di Beranda. */
enum class Urutan {
    TERBARU,
    TERLAMA,
    ABJAD
}

/**
 * Seluruh isi filter Beranda dalam satu objek.
 *
 * Dikemas sebagai satu StateFlow agar perubahan beberapa isian sekaligus
 * (mis. tombol "Terapkan" men-set status + waktu + urutan) hanya memicu satu
 * kali penyaringan, bukan tiga kali.
 */
data class FilterBeranda(
    val type: ItemType? = null,
    val kategori: Set<ReportCategory> = emptySet(),
    val status: ReportStatus? = null,
    val waktu: RentangWaktu = RentangWaktu.SEMUA,
    val pos: String? = null,
    val urutan: Urutan = Urutan.TERBARU
) {

    /** Jumlah isian yang sedang aktif, dipakai sebagai angka di badge filter. */
    val aktif: Int
        get() = listOf(
            type != null,
            kategori.isNotEmpty(),
            status != null,
            waktu != RentangWaktu.SEMUA,
            pos != null,
            urutan != Urutan.TERBARU
        ).count { it }

    /** Benar bila pengguna memakai filter apa pun. */
    val kosong: Boolean get() = aktif == 0
}
