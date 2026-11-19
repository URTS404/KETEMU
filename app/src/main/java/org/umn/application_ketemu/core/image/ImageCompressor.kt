package org.umn.application_ketemu.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Hasil kompresi satu gambar.
 *
 * @param bytes isi berkas siap unggah (sudah tanpa EXIF).
 * @param width lebar setelah diperkecil.
 * @param height tinggi setelah diperkecil.
 */
data class CompressedImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val mimeType: String
) {
    val sizeKb: Int get() = bytes.size / 1024
}

/**
 * Kompresi gambar untuk chat: sinkron dengan pola WhatsApp.
 *
 * Alasan tiap batasan:
 *  - **Sisi terpanjang 1280 px** cukup untuk melihat detail barang (stiker nama,
 *    goresan, warna) tetapi memangkas ukuran 15–25 kali dari foto kamera.
 *  - **WebP q80** (Android 10+) ±30% lebih kecil daripada JPEG dengan tampilan
 *    serupa; JPEG q80 dipakai untuk perangkat lama.
 *  - **EXIF dibuang** karena `Bitmap.compress()` hanya menulis piksel. Foto kamera
 *    menyimpan lokasi GPS di EXIF — ikut terunggah berarti lokasi pemilik bocor.
 *  - **Batas 10 MB** setelah baca, supaya tidak kehabisan memori saat memproses.
 */
object ImageCompressor {

    const val MAX_SIDE = 1280
    const val QUALITY = 80
    private const val MAX_INPUT_BYTES = 10 * 1024 * 1024

    fun compress(context: Context, uri: Uri): CompressedImage? = runCatching {
        val resolver = context.contentResolver

        // 1. Ukur dulu tanpa memuat penuh, agar foto 12 MP tidak langsung OOM.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sisiTerpanjang = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (sisiTerpanjang / (sample * 2) >= MAX_SIDE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val asli = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        if (asli.byteCount > MAX_INPUT_BYTES) {
            asli.recycle()
            return null
        }

        // 2. Perkecil ke sisi terpanjang MAX_SIDE (rasio dipertahankan).
        val skala = MAX_SIDE.toFloat() / max(asli.width, asli.height)
        val bitmap = if (skala < 1f) {
            Bitmap.createScaledBitmap(
                asli,
                (asli.width * skala).toInt().coerceAtLeast(1),
                (asli.height * skala).toInt().coerceAtLeast(1),
                true
            ).also { asli.recycle() }
        } else {
            asli
        }

        // 3. Encode. `compress` hanya menulis piksel → EXIF (termasuk GPS) hilang.
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        val mime = if (format == Bitmap.CompressFormat.WEBP_LOSSY) "image/webp" else "image/jpeg"

        val output = ByteArrayOutputStream()
        val berhasil = runCatching { bitmap.compress(format, QUALITY, output) }.getOrDefault(false)
        val bytes = if (berhasil && output.size() > 0) {
            output.toByteArray()
        } else {
            // Cadangan: JPEG selalu tersedia di semua perangkat Android.
            val jpeg = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, jpeg)
            jpeg.toByteArray()
        }
        val hasil = CompressedImage(
            bytes = bytes,
            width = bitmap.width,
            height = bitmap.height,
            mimeType = if (bytes === output.toByteArray()) "image/jpeg" else mime
        )
        bitmap.recycle()
        hasil
    }.getOrNull()
}
