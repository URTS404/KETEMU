package org.umn.application_ketemu.data.dummy

import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.PosLocation
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole

/**
 * Data contoh untuk fase UI-first.
 *
 * Dipakai agar seluruh layar (kartu barang, timeline status, banner offline,
 * tampilan staff) bisa dibangun, didemokan, dan diuji TANPA menunggu akun
 * Supabase selesai dibuat. Isinya sengaja mencakup keempat status pada PRD
 * plus satu item berstatus PENDING agar badge "Menunggu Sinkron" terlihat.
 *
 * Akan dihapus ketika Room + Supabase aktif (fase M3-M4).
 */
object DummyData {

    /** Master 4 pos fisik sesuai PRD (3 pos gedung + basement). */
    val positions: List<PosLocation> = listOf(
        PosLocation(id = "pos-basement", name = "Pos Basement", building = "Basement Gedung Utama"),
        PosLocation(id = "pos-gedung-b", name = "Pos Gedung B", building = "Lobby Gedung B, Lt. 1"),
        PosLocation(id = "pos-gedung-c", name = "Pos Gedung C", building = "Pintu Utama Gedung C"),
        PosLocation(id = "pos-gedung-d", name = "Pos Gedung D", building = "Lobby Gedung D, Lt. 1")
    )

    val student: UserProfile = UserProfile(
        id = "user-mahasiswa-01",
        fullName = "Budi Santoso",
        email = "budi.santoso@umn.ac.id",
        nimNidn = "00000012345",
        phone = "0812-0000-1111",
        role = UserRole.USER,
        isVerified = true
    )

    val staff: UserProfile = UserProfile(
        id = "user-staff-01",
        fullName = "Pak Rahmat (Satpam)",
        email = "rahmat@umn.ac.id",
        nimNidn = "STAFF-001",
        role = UserRole.STAFF,
        isVerified = true
    )

    /** Dibuat ulang setiap pemanggilan agar waktu relatifnya selalu segar saat demo. */
    fun reports(): List<Report> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        val posBasement = positions[0]
        val posGedungB = positions[1]
        val posGedungC = positions[2]

        return listOf(
            Report(
                id = "rpt-001",
                ownerId = student.id,
                ownerName = student.fullName,
                type = ItemType.LOST,
                title = "Tumbler Hitam Tumbler Angsa",
                description = "Tumbler stainless hitam, ada stiker UMN di badan botol dan nama 'Budi' di bagian bawah.",
                category = ReportCategory.AKSESORIS,
                lastSeenLocation = "Gedung B, ruang B-301",
                status = ReportStatus.DILAPORKAN,
                syncStatus = SyncStatus.SYNCED,
                createdAt = now - 2 * hour
            ),
            Report(
                id = "rpt-002",
                ownerId = "user-lain-02",
                ownerName = "Sinta Maharani",
                type = ItemType.FOUND,
                title = "KTM atas nama Andi Wijaya",
                description = "Kartu tanda mahasiswa ditemukan di kursi lobby, diserahkan ke pos untuk pengambilan.",
                category = ReportCategory.DOKUMEN,
                lastSeenLocation = "Lobby Gedung B",
                pos = posGedungB,
                status = ReportStatus.DISERAHKAN_KE_POS,
                syncStatus = SyncStatus.SYNCED,
                createdAt = now - 5 * hour
            ),
            Report(
                id = "rpt-003",
                ownerId = "user-lain-03",
                ownerName = "Andi Wijaya",
                type = ItemType.FOUND,
                title = "AirPods Gen 2 (case putih)",
                description = "Earbuds putih di dalam case, ditemukan di meja perpustakaan lantai 4.",
                category = ReportCategory.ELEKTRONIK,
                lastSeenLocation = "Perpustakaan, Lt. 4",
                pos = posBasement,
                status = ReportStatus.SIAP_DIAMBIL,
                syncStatus = SyncStatus.SYNCED,
                createdAt = now - 26 * hour
            ),
            Report(
                id = "rpt-004",
                ownerId = "user-lain-04",
                ownerName = "Dewi Lestari",
                type = ItemType.LOST,
                title = "Dompet Cokelat Berisi KTM",
                description = "Dompet kulit cokelat, berisi KTM dan kartu ATM. Terakhir dipakai di kantin.",
                category = ReportCategory.DOKUMEN,
                lastSeenLocation = "Kantin Gedung D",
                pos = posGedungC,
                status = ReportStatus.SELESAI_DIKLAIM,
                syncStatus = SyncStatus.SYNCED,
                createdAt = now - 3 * day
            ),
            Report(
                id = "rpt-005",
                ownerId = "user-lain-05",
                ownerName = "Rizky Pratama",
                type = ItemType.FOUND,
                title = "Payung Lipat Biru",
                description = "Payung lipat warna biru tua, gagang kayu, ditemukan di halte depan kampus.",
                category = ReportCategory.LAINNYA,
                lastSeenLocation = "Halte depan kampus",
                pos = posBasement,
                status = ReportStatus.DISERAHKAN_KE_POS,
                syncStatus = SyncStatus.SYNCED,
                createdAt = now - 2 * day
            ),
            Report(
                id = "rpt-006",
                ownerId = student.id,
                ownerName = student.fullName,
                type = ItemType.LOST,
                title = "Laptop Sleeve Abu-abu",
                description = "Sleeve laptop 14 inci dengan gantungan kunci karakter di resleting. Dibuat saat sinyal kampus sedang mati.",
                category = ReportCategory.AKSESORIS,
                lastSeenLocation = "Gedung A, ruang A-202",
                status = ReportStatus.DILAPORKAN,
                syncStatus = SyncStatus.PENDING,
                createdAt = now - 12 * minute
            )
        )
    }
}
