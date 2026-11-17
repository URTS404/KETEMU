package org.umn.application_ketemu.ui.feature.admin

/**
 * Teks yang dipakai bersama oleh ViewModel panel admin.
 *
 * Disimpan sebagai konstanta (bukan `stringResource`) karena ViewModel tidak
 * memegang Context; layar admin menampilkan pesan apa adanya pada snackbar.
 */
internal object AdminUiText {
    const val ACCESS_DENIED = "Akses ditolak: panel ini hanya untuk peran STAFF/ADMIN."
    const val REPORT_NOT_FOUND = "Laporan tidak ditemukan."
    const val CLAIM_NOT_FOUND = "Klaim tidak ditemukan."
    const val USER_NOT_FOUND = "Akun tidak ditemukan."

    /** Satpam menekan konfirmasi tanpa memilih pos tujuan. */
    const val POS_REQUIRED = "Pilih pos tujuan sebelum meneruskan laporan."

    /**
     * Laporan kehilangan tidak punya barang fisik yang bisa dititipkan, jadi
     * tidak boleh diserahkan ke Pos. Petugas hanya boleh mengarsipkannya bila
     * laporan tidak pantas ditampilkan.
     */
    const val LOST_NOT_FORWARDABLE =
        "Laporan kehilangan tidak perlu diserahkan ke Pos. Gunakan Arsipkan bila laporan tidak pantas."

    /**
     * Kegagalan jaringan/server. Tanpa pesan ini, pengecualian PostgREST akan
     * naik ke `viewModelScope.launch` dan mematikan aplikasi (FATAL EXCEPTION),
     * bukan sekadar menampilkan snackbar.
     */
    const val ACTION_FAILED = "Aksi gagal diproses server. Periksa koneksi lalu coba lagi."
}
