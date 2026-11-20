package org.umn.application_ketemu.core.visual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Uji hitungan vektor untuk fitur cari-dengan-foto.
 *
 * Yang dijaga di sini adalah kesepakatan dengan database: vektor harus
 * ternormalisasi L2, karena kolom `photo_embedding` memakai jarak kosinus
 * pgvector. Kalau normalisasi hilang, urutan hasil pencarian di server berubah
 * tanpa ada galat apa pun — kegagalan yang paling sulit dilacak.
 */
class MatematikaVektorTest {

    @Test
    fun `normalisasi menghasilkan panjang satu`() {
        val vektor = MatematikaVektor.normalisasi(floatArrayOf(3f, 4f))

        val panjang = kotlin.math.sqrt((vektor[0] * vektor[0] + vektor[1] * vektor[1]).toDouble())
        assertEquals(1.0, panjang, 1e-6)
        assertEquals(0.6f, vektor[0], 1e-6f)
        assertEquals(0.8f, vektor[1], 1e-6f)
    }

    @Test
    fun `normalisasi vektor nol tidak membagi nol`() {
        val vektor = MatematikaVektor.normalisasi(floatArrayOf(0f, 0f, 0f))

        assertEquals(0f, vektor[0], 0f)
        assertEquals(0f, vektor[1], 0f)
        assertEquals(0f, vektor[2], 0f)
    }

    @Test
    fun `vektor searah bernilai satu dan tegak lurus bernilai nol`() {
        val a = MatematikaVektor.normalisasi(floatArrayOf(1f, 0f))
        val b = MatematikaVektor.normalisasi(floatArrayOf(2f, 0f))
        val c = MatematikaVektor.normalisasi(floatArrayOf(0f, 5f))

        assertEquals(1f, MatematikaVektor.kemiripan(a, b), 1e-6f)
        assertEquals(0f, MatematikaVektor.kemiripan(a, c), 1e-6f)
    }

    @Test
    fun `vektor berlawanan bernilai minus satu`() {
        val a = MatematikaVektor.normalisasi(floatArrayOf(1f, 2f, 3f))
        val b = a.map { -it }.toFloatArray()

        assertEquals(-1f, MatematikaVektor.kemiripan(a, b), 1e-6f)
    }

    @Test
    fun `kemiripan vektor berbeda panjang memakai bagian terpendek`() {
        val a = MatematikaVektor.normalisasi(floatArrayOf(1f, 0f, 0f, 0f))
        val b = MatematikaVektor.normalisasi(floatArrayOf(1f, 0f))

        assertTrue(abs(MatematikaVektor.kemiripan(a, b) - 1f) < 1e-6f)
    }
}
