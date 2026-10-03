package org.umn.application_ketemu.domain.model

/**
 * Status sinkronisasi sebuah record terhadap Supabase.
 *
 * Ini kunci arsitektur offline-first pada PRD: data SELALU dibuat di Room
 * (lokal) lebih dulu, lalu diberi tanda PENDING sampai WorkManager berhasil
 * mengunggahnya ke server.
 */
enum class SyncStatus {
    /** Sudah sama dengan server. */
    SYNCED,

    /** Dibuat/diubah saat offline — menunggu dikirim WorkManager. */
    PENDING,

    /** Gagal setelah beberapa kali percobaan — perlu perhatian pengguna. */
    FAILED
}
