package org.umn.application_ketemu.core.util

import java.util.concurrent.TimeUnit

/**
 * Pemformat waktu relatif berbahasa Indonesia ("2 jam lalu").
 *
 * Dipakai agar kartu laporan mudah dipindai cepat oleh pengguna (PRD: antarmuka
 * harus cepat dipahami, termasuk oleh dosen senior).
 *
 * Catatan: pada M2 utilitas ini akan dipindahkan ke `pluralStringResource`
 * supaya jumlah jam/hari mengikuti aturan pluralisasi resource string.
 */
object RelativeTime {

    fun format(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - epochMillis).coerceAtLeast(0L)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Baru saja"
            minutes < 60 -> "$minutes menit lalu"
            hours < 24 -> "$hours jam lalu"
            days < 30 -> "$days hari lalu"
            else -> "${days / 30} bulan lalu"
        }
    }
}
