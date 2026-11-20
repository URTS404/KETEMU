package org.umn.application_ketemu.data.sync

import android.content.Context

/**
 * Penanda waktu notifikasi terakhir yang sudah dibunyikan pekerja latar.
 *
 * Disimpan di SharedPreferences (bukan di Room) karena nilainya sangat kecil,
 * hanya dibaca/ditulis pekerja latar, dan tidak perlu ikut disinkronkan. Tanpa
 * penanda ini, [NotifikasiSyncWorker] yang berjalan tiap 15 menit akan
 * membunyikan ulang seluruh isi kotak masuk setiap kali dijalankan.
 */
class PenandaNotifikasi(context: Context) {

    private val preferensi =
        context.getSharedPreferences(NAMA_BERKAS, Context.MODE_PRIVATE)

    /** Waktu (epoch millis) notifikasi terakhir yang sudah ditampilkan. */
    var terakhirMs: Long
        get() = preferensi.getLong(KUNCI_TERAKHIR, 0L)
        set(nilai) {
            preferensi.edit().putLong(KUNCI_TERAKHIR, nilai).apply()
        }

    private companion object {
        const val NAMA_BERKAS = "notifikasi_latar"
        const val KUNCI_TERAKHIR = "terakhir_ms"
    }
}
