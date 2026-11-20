package org.umn.application_ketemu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu aksi tulis yang belum berhasil dikirim ke server (antrean "outbox").
 *
 * Alurnya: aksi pengguna ditulis ke `reports_cache` (bertanda PENDING) **dan**
 * ke tabel ini dalam satu transaksi, sehingga offline sekalipun aksi tidak
 * hilang. Saat jaringan tersedia, [org.umn.application_ketemu.data.repository.RoomReportRepository]
 * mengirim baris-baris ini berurutan lalu menghapusnya.
 *
 * @param kind jenis aksi, lihat [OutboxKind].
 * @param payload JSON berisi parameter aksi (mis. pos tujuan saat serah-terima).
 * @param attempts jumlah percobaan gagal; dipakai untuk menampilkan pesan
 *                 "menunggu sinkron" yang jujur dan untuk menghindari
 *                 percobaan tanpa henti pada aksi yang ditolak server.
 */
@Entity(tableName = "outbox", indices = [androidx.room.Index("reportId")])
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val reportId: String,
    val payload: String,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long
)

/** Jenis aksi yang bisa masuk antrean. */
enum class OutboxKind {
    /** Laporan baru belum ada di server. */
    CREATE_REPORT,

    /** Perubahan status barang (mis. DIAMBIL, SIAP_DIAMBIL). */
    UPDATE_STATUS,

    /** Laporan disembunyikan / dimunculkan moderator. */
    ARCHIVE,

    /** Serah-terima fisik ke pos oleh petugas. */
    HANDOVER
}
