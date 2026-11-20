package org.umn.application_ketemu.ui.feature.detail

/**
 * Teks yang dipakai bersama oleh ViewModel halaman detail.
 *
 * Disimpan sebagai konstanta (bukan `stringResource`) karena ViewModel tidak
 * memegang Context; layar menampilkan pesan apa adanya pada snackbar. Pola ini
 * sama dengan `AdminUiText` pada panel admin.
 */
internal object ClaimUiText {
    /** Panjang minimum ciri barang agar petugas punya dasar menilai klaim. */
    const val MIN_ANSWER_LENGTH = 15

    const val REPORT_NOT_FOUND = "Laporan tidak ditemukan."
    const val OWN_REPORT = "Ini laporan Anda sendiri, jadi tidak perlu diklaim."
    const val NOT_AT_POS =
        "Klaim hanya bisa diajukan untuk barang temuan yang sudah dititipkan di pos."
    const val ALREADY_SUBMITTED =
        "Anda sudah mengajukan klaim untuk barang ini. Tunggu keputusan petugas."
    /** Klaim lama masih MENUNGGU/DISETUJUI, jadi belum boleh mengajukan lagi. */
    const val ALREADY_ACTIVE =
        "Klaim Anda untuk barang ini masih aktif. Cek statusnya di halaman ini."
    const val ANSWER_TOO_SHORT =
        "Tulis ciri barang minimal 15 karakter agar petugas bisa memverifikasi."
    const val SUBMITTED =
        "Klaim terkirim. Petugas akan mencocokkan ciri barang sebelum menyerahkannya."
    const val SUBMIT_FAILED =
        "Klaim gagal dikirim. Periksa koneksi lalu coba lagi."

    /** Penghapusan laporan ditolak karena laporan masih diproses petugas. */
    const val DELETE_BLOCKED_BY_CLAIM =
        "Laporan belum bisa dihapus: masih ada klaim yang diproses petugas, atau laporan ini bukan milik akun Anda."

    const val DELETE_FAILED =
        "Laporan gagal dihapus. Periksa koneksi lalu coba lagi."
}
