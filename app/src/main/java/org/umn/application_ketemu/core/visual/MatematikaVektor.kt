package org.umn.application_ketemu.core.visual

import kotlin.math.sqrt

/**
 * Hitungan vektor murni (tanpa Android) agar bisa diuji di JVM.
 *
 * Dipisahkan dari [PencariVisual] yang memegang interpreter TensorFlow Lite,
 * karena interpreter itu butuh pustaka native dan tidak bisa dijalankan di unit
 * test biasa.
 */
object MatematikaVektor {

    /**
     * Normalisasi L2: hasilnya punya panjang 1, sehingga kemiripan kosinus sama
     * dengan hasil kali titik — persis operator `<=>` di pgvector.
     */
    fun normalisasi(vektor: FloatArray): FloatArray {
        var total = 0.0
        for (nilai in vektor) total += nilai.toDouble() * nilai
        if (total <= 0.0) return vektor
        val akar = sqrt(total).toFloat()
        for (i in vektor.indices) vektor[i] = vektor[i] / akar
        return vektor
    }

    /** Kemiripan kosinus dua vektor yang sudah dinormalisasi (rentang -1..1). */
    fun kemiripan(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var hasil = 0f
        for (i in 0 until n) hasil += a[i] * b[i]
        return hasil
    }
}
