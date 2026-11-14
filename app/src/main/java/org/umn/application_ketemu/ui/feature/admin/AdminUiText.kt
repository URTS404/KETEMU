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
}
