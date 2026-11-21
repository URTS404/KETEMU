package org.umn.application_ketemu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Akses tabel `reports_cache` (salinan lokal laporan). */
@Dao
interface ReportDao {

    /** Seluruh laporan, terbaru di depan. Dipakai Beranda. */
    @Query("SELECT * FROM reports_cache ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    /** Satu laporan; null bila belum pernah tersimpan di perangkat. */
    @Query("SELECT * FROM reports_cache WHERE id = :id")
    suspend fun findById(id: String): ReportEntity?

    /** Menyimpan/menimpa satu atau banyak baris. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<ReportEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ReportEntity)

    /** Id laporan yang masih punya antrean (tidak boleh ditimpa hasil server). */
    @Query("SELECT id FROM reports_cache WHERE syncStatus = 'PENDING'")
    suspend fun pendingIds(): List<String>

    /**
     * Membuang baris SYNCED yang sudah tidak ada di server (laporan dihapus atau
     * tidak lagi boleh dilihat pengguna). Baris PENDING sengaja dipertahankan
     * karena perubahannya belum sempat terkirim.
     */
    @Query("DELETE FROM reports_cache WHERE syncStatus = 'SYNCED' AND id NOT IN (:ids)")
    suspend fun deleteSyncedNotIn(ids: List<String>)

    @Query("UPDATE reports_cache SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM reports_cache WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM reports_cache")
    suspend fun count(): Int

    /**
     * Laporan milik pengguna yang fotonya masih menunjuk berkas lokal perangkat
     * (`content://` / `file://`). Baris seperti ini tidak bisa dibuka di perangkat
     * lain, jadi dicoba diunggah ulang ke Storage saat aplikasi dibuka pemiliknya.
     */
    @Query(
        "SELECT * FROM reports_cache " +
            "WHERE ownerId = :ownerId AND photoUri IS NOT NULL AND photoUri NOT LIKE 'http%'"
    )
    suspend fun fotoLokalMilik(ownerId: String): List<ReportEntity>
}
