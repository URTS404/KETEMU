package org.umn.application_ketemu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Akses antrean tulis yang belum terkirim (tabel `outbox`). */
@Dao
interface OutboxDao {

    /** Antrean terlama dulu: aksi dikirim berurutan agar tidak saling menimpa. */
    @Query("SELECT * FROM outbox ORDER BY id ASC")
    fun observeAll(): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM outbox ORDER BY id ASC LIMIT 1")
    suspend fun first(): OutboxEntity?

    @Insert
    suspend fun insert(entry: OutboxEntity): Long

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE outbox SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun recordFailure(id: Long, error: String?)

    /** Aksi milik satu laporan (dipakai saat memuat ulang detail). */
    @Query("SELECT COUNT(*) FROM outbox WHERE reportId = :reportId")
    suspend fun countFor(reportId: String): Int

    /** Membuang semua aksi tertunda milik satu laporan (dipakai saat laporan dihapus). */
    @Query("DELETE FROM outbox WHERE reportId = :reportId")
    suspend fun deleteFor(reportId: String)

    @Query("SELECT COUNT(*) FROM outbox")
    fun observeCount(): Flow<Int>
}
