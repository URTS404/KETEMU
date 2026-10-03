package org.umn.application_ketemu.core.common

/**
 * State baku untuk seluruh layar.
 *
 * PRD menekankan aplikasi tidak boleh "kosong" atau menampilkan layar putih;
 * karena itu setiap layar wajib bisa merender keempat state ini.
 */
sealed interface UiState<out T> {

    /** Sedang memuat (di UI ditampilkan skeleton/shimmer, bukan spinner kosong). */
    data object Loading : UiState<Nothing>

    /** Data tersedia (bisa dari cache Room meski sedang offline). */
    data class Success<T>(val data: T) : UiState<T>

    /** Permintaan berhasil tetapi tidak ada data — tetap tampilkan panduan aksi. */
    data object Empty : UiState<Nothing>

    /** Gagal (mis. koneksi putus). Pesan boleh dikosongkan agar UI memakai teks baku. */
    data class Error(val message: String? = null) : UiState<Nothing>
}
