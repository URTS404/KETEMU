package org.umn.application_ketemu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Basis data lokal KETEMU (SQLite lewat Room).
 *
 * `reports_cache` = salinan laporan dari server + perubahan lokal yang belum
 * terkirim; `outbox` = antrean tulis. Keduanya ditulis dalam satu transaksi
 * oleh [org.umn.application_ketemu.data.repository.RoomReportRepository],
 * sehingga aplikasi tetap konsisten saat offline.
 *
 * Versi dinaikkan 1 setiap kali skema berubah; jangan lupa menambah migrasi
 * (atau `fallbackToDestructiveMigration` di build debug) agar data pengguna
 * tidak hilang.
 */
@Database(
    entities = [ReportEntity::class, OutboxEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KetemuDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao

    abstract fun outboxDao(): OutboxDao

    companion object {
        private const val NAMA_BERKAS = "ketemu.db"

        @Volatile
        private var instance: KetemuDatabase? = null

        /** Satu instance untuk seluruh proses (Room mahal bila dibuka berkali-kali). */
        fun get(context: Context): KetemuDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): KetemuDatabase =
            Room.databaseBuilder(context, KetemuDatabase::class.java, NAMA_BERKAS)
                // Skema masih versi 1 dan aplikasi belum dirilis: bila bentuk tabel
                // berubah saat pengembangan, bangun ulang basis data daripada
                // memaksa migrasi yang belum ada. GANTI dengan migrasi sungguhan
                // sebelum rilis ke pengguna.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
