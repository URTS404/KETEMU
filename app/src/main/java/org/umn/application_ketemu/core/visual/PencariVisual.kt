package org.umn.application_ketemu.core.visual

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

import org.tensorflow.lite.Interpreter

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

/**
 * Pengubah foto menjadi vektor angka ("embedding") untuk fitur cari-dengan-foto.
 *
 * Semua perhitungan berjalan di dalam HP memakai model MobileNetV3 yang ikut
 * dibundel di APK — tidak ada foto yang dikirim ke server mana pun. Vektor hasil
 * perhitungan inilah yang dikirim ke database saat membuat laporan temuan,
 * sehingga pencarian kemiripan bisa memakai pgvector tanpa membocorkan foto.
 *
 * Catatan desain:
 *  - Normalisasi input [-1, 1] mengikuti kebiasaan model MobileNet; konsistensi
 *    lebih penting daripada presisi, karena pembandingnya adalah vektor yang
 *    dihitung dengan jalur kode yang sama.
 *  - Vektor keluaran dinormalisasi L2 supaya kemiripan kosinus sama dengan
 *    perkalian titik (operator `<=>` milik pgvector).
 */
class PencariVisual private constructor(context: Context) : AutoCloseable {

    private val interpreter: Interpreter

    /** Dimensi vektor yang dihasilkan model (dibaca dari bentuk keluaran model). */
    val dimensi: Int

    /** Ukuran sisi gambar yang diterima model (persegi). */
    private val sisiMasuk: Int

    init {
        val berkas = context.assets.open(NAMA_MODEL).use { aliran ->
            val keluaran = ByteArrayOutputStream(aliran.available())
            aliran.copyTo(keluaran)
            keluaran.toByteArray()
        }
        val penyangga = ByteBuffer.allocateDirect(berkas.size)
            .order(ByteOrder.nativeOrder())
            .put(berkas)
        penyangga.rewind()

        interpreter = Interpreter(
            penyangga,
            Interpreter.Options().apply { setNumThreads(UTAS) },
        )

        val bentukMasuk = interpreter.getInputTensor(0).shape()   // [1, tinggi, lebar, 3]
        val bentukKeluar = interpreter.getOutputTensor(0).shape() // [1, dimensi]
        sisiMasuk = bentukMasuk[1]
        dimensi = bentukKeluar[bentukKeluar.lastIndex]
        Log.d(
            TAG,
            "model $NAMA_MODEL siap: masuk=${bentukMasuk.toList()} keluar=${bentukKeluar.toList()}",
        )
    }

    /**
     * Menghitung vektor untuk [bitmap]. Hasil sudah dinormalisasi L2 (panjang 1),
     * jadi kemiripan kosinus = hasil kali titik.
     */
    fun vektor(bitmap: Bitmap): FloatArray {
        val kecil = Bitmap.createScaledBitmap(bitmap, sisiMasuk, sisiMasuk, true)
        val piksel = IntArray(sisiMasuk * sisiMasuk)
        kecil.getPixels(piksel, 0, sisiMasuk, 0, 0, sisiMasuk, sisiMasuk)
        if (kecil !== bitmap) kecil.recycle()

        val masukan = ByteBuffer
            .allocateDirect(BYTES_PER_FLOAT * sisiMasuk * sisiMasuk * KANAL)
            .order(ByteOrder.nativeOrder())
        for (warna in piksel) {
            val merah = (warna shr 16) and 0xFF
            val hijau = (warna shr 8) and 0xFF
            val biru = warna and 0xFF
            masukan.putFloat(merah / SKALA - 1f)
            masukan.putFloat(hijau / SKALA - 1f)
            masukan.putFloat(biru / SKALA - 1f)
        }
        masukan.rewind()

        val keluaran = Array(1) { FloatArray(dimensi) }
        interpreter.run(masukan, keluaran)
        return normalisasi(keluaran[0])
    }

    override fun close() {
        synchronized(kunci) { if (contoh === this) contoh = null }
        runCatching { interpreter.close() }
    }

    companion object {
        private const val TAG = "Lens"
        private const val NAMA_MODEL = "mobilenet_v3_small.tflite"
        private const val UTAS = 2
        private const val KANAL = 3
        private const val BYTES_PER_FLOAT = 4
        private const val SKALA = 127.5f

        private val kunci = Any()
        private var contoh: PencariVisual? = null

        /**
         * Satu contoh per proses: memuat model memakan waktu dan puluhan MB,
         * jadi jangan dibuat ulang setiap kali pengguna mencari.
         */
        fun ambil(context: Context): PencariVisual = synchronized(kunci) {
            contoh ?: PencariVisual(context).also { contoh = it }
        }

        /** Normalisasi L2; lihat [MatematikaVektor.normalisasi]. */
        fun normalisasi(vektor: FloatArray): FloatArray = MatematikaVektor.normalisasi(vektor)

        /** Kemiripan kosinus; lihat [MatematikaVektor.kemiripan]. */
        fun kemiripan(a: FloatArray, b: FloatArray): Float = MatematikaVektor.kemiripan(a, b)

        /** Dipakai di luar alur normal saat model perlu dijalankan di latar. */
        val eksekutor by lazy { Executors.newSingleThreadExecutor() }
    }
}
