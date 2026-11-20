package org.umn.application_ketemu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Basis data lokal KETEMU (SQLite lewat Room).
 *
 * `reports_cache` = salinan laporan dari server + perubahan lokal yang belum
 * terkirim; `outbox` = antrean tulis. Keduanya ditulis dalam satu transaksi
 * oleh [org.umn.application_ketemu.data.repository.RoomReportRepository],
 * sehingga aplikasi tetap konsisten saat offline.
 *
 * ## Aturan perubahan skema
 *
 * Versi dinaikkan satu setiap kali bentuk tabel berubah, dan versi baru WAJIB
 * disertai [Migration] di [MIGRASI]. Tidak ada lagi
 * `fallbackToDestructiveMigration`: kalau migrasi lupa ditulis, aplikasi gagal
 * keras saat dibuka (mudah ditemukan saat pengujian) alih-alih diam-diam
 * menghapus laporan offline dan antrean yang belum terkirim milik pengguna.
 *
 * Langkah menambah versi:
 * 1. ubah entity + naikkan `version`
 * 2. tulis `Migration(lama, baru)` di [MIGRASI]
 * 3. jalankan `:app:compileDebugKotlin` agar `app/schemas/<versi>.json` ikut
 *    tergenerasi dan ikut di-commit
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

        /**
         * Daftar migrasi skema, diurutkan dari versi lama ke baru.
         *
         * Kosong karena skema masih versi 1. Contoh bentuk nanti:
         * ```
         * private val DARI_1_KE_2 = object : Migration(1, 2) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("alter table reports_cache add column catatan text")
         *     }
         * }
         * private val MIGRASI = arrayOf(DARI_1_KE_2)
         * ```
         */
        private val MIGRASI: Array<Migration> = emptyArray()

        @Volatile
        private var instance: KetemuDatabase? = null

        /** Satu instance untuk seluruh proses (Room mahal bila dibuka berkali-kali). */
        fun get(context: Context): KetemuDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): KetemuDatabase =
            Room.databaseBuilder(context, KetemuDatabase::class.java, NAMA_BERKAS)
                .addMigrations(*MIGRASI)
                .build()
    }
}
