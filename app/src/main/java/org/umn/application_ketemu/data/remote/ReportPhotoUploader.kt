package org.umn.application_ketemu.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import org.umn.application_ketemu.core.image.ImageCompressor
import java.util.UUID

/**
 * Mengunggah foto laporan ke bucket publik `foto-laporan` (db/0033).
 *
 * Sebelum ini kolom `reports.photo_url` berisi URI lokal perangkat
 * (`content://media/picker/...`) sehingga foto hanya terlihat oleh pelapor.
 * Fungsi ini menggantinya dengan URL https yang bisa dibaca semua pengguna —
 * penting agar kartu berfoto di Beranda dan pencarian visual (Lens) berguna.
 *
 * Foto dikompres dulu (maks sisi 1280 px, WebP q80) supaya hemat kuota dan
 * di bawah batas 5 MB bucket. Bila unggahan gagal (offline, sesi habis), foto
 * URI lokal dikembalikan apa adanya agar laporan tetap bisa disimpan.
 */
object ReportPhotoUploader {

    const val BUCKET = "foto-laporan"

    private const val TAG = "Laporan"

    suspend fun unggah(context: Context, uriTeks: String): String? = runCatching {
        // URL http sudah berarti foto pernah terunggah (mis. laporan lama).
        if (uriTeks.startsWith("http")) return uriTeks

        val client = SupabaseProvider.client
        val userId = client.auth.currentUserOrNull()?.id ?: return null

        val gambar = ImageCompressor.compress(context, Uri.parse(uriTeks))
            ?: return null

        val path = "$userId/${UUID.randomUUID()}.webp"
        client.storage.from(BUCKET).upload(path, gambar.bytes) { upsert = false }
        val url = client.storage.from(BUCKET).publicUrl(path)

        Log.d(TAG, "foto terunggah: $url (${gambar.sizeKb} KB)")
        url
    }.getOrElse { galat ->
        Log.w(TAG, "unggah foto gagal: ${galat.message}")
        null
    }
}
