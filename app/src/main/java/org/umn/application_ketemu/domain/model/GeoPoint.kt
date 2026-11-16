package org.umn.application_ketemu.domain.model

import java.util.Locale

/**
 * Titik koordinat GPS untuk menandai tempat barang terakhir terlihat/ditemukan.
 *
 * Dipisahkan dari [Report] agar validasi dan pemformatan koordinat punya satu
 * sumber kebenaran, baik saat data datang dari GPS, dari peta, maupun dari
 * kolom `latitude`/`longitude` di Supabase.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {

    /** `false` untuk nilai NaN/tak hingga atau di luar rentang bumi. */
    val isValid: Boolean
        get() = latitude.isFinite() && longitude.isFinite() &&
            latitude in MIN_LATITUDE..MAX_LATITUDE &&
            longitude in MIN_LONGITUDE..MAX_LONGITUDE

    /**
     * Teks koordinat 6 desimal (presisi ±0,1 m) dengan pemisah titik.
     * Memakai [Locale.US] supaya hasilnya sama di semua locale perangkat dan
     * aman dipakai sebagai isi kolom teks lokasi.
     */
    fun toCoordinateText(): String =
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)

    companion object {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0

        /**
         * Titik awal peta sebelum pengguna memilih lokasi (kampus UMN Gading
         * Serpong). Hanya untuk tampilan awal, bukan data laporan.
         */
        val CAMPUS_CENTER = GeoPoint(latitude = -6.257700, longitude = 106.618200)
    }
}
