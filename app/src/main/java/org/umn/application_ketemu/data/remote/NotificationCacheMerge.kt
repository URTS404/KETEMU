package org.umn.application_ketemu.data.remote

import org.umn.application_ketemu.domain.model.AppNotification

/**
 * Menggabungkan satu kabar yang baru tiba (mis. dari Supabase Realtime) ke
 * daftar cache kotak masuk, tanpa menghasilkan baris ganda.
 *
 * Aturan:
 *  - `id` yang sudah ada diabaikan. Baris yang sama bisa dikirim lebih dari
 *    sekali (Realtime dan polling `refresh()` berjalan berdampingan); salinan
 *    yang sudah tersimpan dianggap lebih baru karena memuat `is_read` terkini.
 *  - Hasil selalu urut dari yang terbaru, sama seperti hasil `refresh()`.
 *
 * Fungsi murni supaya aturannya bisa diuji tanpa jaringan maupun klien Supabase.
 */
internal fun List<AppNotification>.mergedWith(incoming: AppNotification): List<AppNotification> {
    if (any { it.id == incoming.id }) return this
    return (this + incoming).sortedByDescending { it.createdAt }
}
