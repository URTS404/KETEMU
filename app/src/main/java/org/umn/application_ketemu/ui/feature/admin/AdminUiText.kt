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

    /** Serah terima berhasil: laporan temuan & hilang pasangannya sudah ditutup. */
    const val HANDOVER_DONE =
        "Barang tercatat sudah diserahkan. Laporan temuan dan laporan hilang terkait ditutup."

    /** Serah terima ditolak server sebelum sempat berubah apa pun. */
    const val HANDOVER_FAILED =
        "Serah terima gagal. Pastikan klaim sudah disetujui dan barang masih di pos."

    /** Kode yang disebut pemilik tidak sama dengan kode pengambilan di sistem. */
    const val HANDOVER_CODE_WRONG =
        "Kode pengambilan tidak cocok. Periksa kembali kode yang disebut pemilik."

    /** Klaim belum disetujui, jadi barangnya belum boleh diserahkan. */
    const val HANDOVER_NOT_APPROVED =
        "Klaim ini belum disetujui. Setujui klaim lebih dulu sebelum menyerahkan barang."

    /** Laporan belum berstatus siap diambil (mis. barang belum diserahkan ke pos). */
    const val HANDOVER_NOT_READY =
        "Barang belum berstatus siap diambil. Verifikasi laporan temuan dan pilih pos tujuan dulu."
}
