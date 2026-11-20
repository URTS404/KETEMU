package org.umn.application_ketemu.core.visual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Penghubung antara URI foto dan [PencariVisual].
 *
 * Dipanggil setelah laporan berhasil disimpan (atau saat pengguna mencari dengan
 * foto). Semua pembacaan berkas dan perhitungan model dijalankan di [Dispatchers.IO]
 * karena dekode gambar bisa memakan puluhan milidetik.
 *
 * Kegagalan di sini TIDAK boleh menggagalkan alur utama: laporan tetap sah walau
 * sidik fotonya tidak terbentuk, hanya fitur pencarian foto yang kehilangan satu
 * pembanding.
 */
object VektorFoto {

    private const val TAG = "Lens"

    /** Batas sisi gambar agar tidak kehabisan memori saat mendekode foto kamera. */
    private const val SISI_MAKS = 1600

    /**
     * Menghitung vektor dari foto di [uri] (boleh `content://` maupun `file://`).
     * Mengembalikan `null` bila gambar gagal dibaca atau model tidak tersedia.
     */
    suspend fun hitung(context: Context, uri: String): FloatArray? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = bacaBitmap(context, uri) ?: return@runCatching null
            try {
                PencariVisual.ambil(context).vektor(bitmap)
            } finally {
                bitmap.recycle()
            }
        }.onFailure {
            Log.w(TAG, "vektor foto gagal dibuat untuk $uri: ${it.message}")
        }.getOrNull()
    }

    private fun bacaBitmap(context: Context, uri: String): Bitmap? {
        val alamat = Uri.parse(uri)

        // Ukuran asli dibaca lebih dulu supaya foto 12 MP tidak didekode utuh.
        val opsi = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(alamat)?.use {
            BitmapFactory.decodeStream(it, null, opsi)
        }
        if (opsi.outWidth <= 0 || opsi.outHeight <= 0) return null

        var skala = 1
        while (opsi.outWidth / skala > SISI_MAKS || opsi.outHeight / skala > SISI_MAKS) {
            skala *= 2
        }

        val opsiNyata = BitmapFactory.Options().apply { inSampleSize = skala }
        return context.contentResolver.openInputStream(alamat)?.use {
            BitmapFactory.decodeStream(it, null, opsiNyata)
        }
    }
}
