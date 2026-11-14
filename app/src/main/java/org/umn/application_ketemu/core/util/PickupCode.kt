package org.umn.application_ketemu.core.util

/**
 * Pembuat kode pengambilan barang (6 karakter).
 *
 * Sengaja deterministik terhadap [seed] supaya hasilnya bisa diuji dan, saat
 * data pindah ke Supabase, kode yang sama dapat dihitung ulang dari id klaim
 * tanpa kolom tambahan. Alfabet membuang karakter yang mudah tertukar saat
 * dibacakan (I, O, 0, 1).
 */
object PickupCode {

    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 6

    // FNV-1a 32-bit dalam bentuk Int (offset basis 0x811C9DC5, prime 16777619).
    private const val FNV_OFFSET = -2128831035
    private const val FNV_PRIME = 16777619
    private const val REHASH = 0x9E3779B9L

    fun generate(seed: String): String {
        var hash = FNV_OFFSET
        for (char in seed) {
            hash = hash xor char.code
            hash *= FNV_PRIME
        }

        var value = hash.toLong() and 0xFFFFFFFFL
        val builder = StringBuilder(LENGTH)
        repeat(LENGTH) { index ->
            builder.append(ALPHABET[(value % ALPHABET.length).toInt()])
            value /= ALPHABET.length
            if (value == 0L) {
                // Putar ulang agar tetap 6 karakter meski hash bernilai kecil.
                value = (hash.toLong() and 0xFFFFFFFFL) xor (REHASH * (index + 1))
            }
        }
        return builder.toString()
    }
}
