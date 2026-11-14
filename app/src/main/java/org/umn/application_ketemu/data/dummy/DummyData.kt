package org.umn.application_ketemu.data.dummy

import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.ClaimStatus
import org.umn.application_ketemu.domain.model.ConversationThread
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
        email = "budi.santoso@student.umn.ac.id",
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

    /**
     * Klaim contoh untuk antrean admin.
     *
     * Dua klaim menunggu keputusan (menempel pada laporan temuan yang sudah
     * diserahkan ke pos) dan satu klaim yang sudah disetujui sebagai riwayat.
     * Dengan begitu layar admin punya isi sejak demo pertama.
     */
    fun claims(): List<Claim> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        return listOf(
            Claim(
                id = "clm-001",
                reportId = "rpt-002",
                reportTitle = "KTM atas nama Andi Wijaya",
                claimantId = "user-lain-03",
                claimantName = "Andi Wijaya",
                claimantEmail = "andi.wijaya@student.umn.ac.id",
                answer = "NIM saya 00000077777 dan foto di kartu sesuai dengan wajah saya.",
                createdAt = now - 45 * minute
            ),
            Claim(
                id = "clm-002",
                reportId = "rpt-005",
                reportTitle = "Payung Lipat Biru",
                claimantId = "user-lain-06",
                claimantEmail = "rani.kusuma@student.umn.ac.id",
                claimantName = "Rani Kusuma",
                answer = "Gagang kayunya ada tulisan nama saya dengan spidol hitam.",
                createdAt = now - 3 * hour
            ),
            Claim(
                id = "clm-003",
                reportId = "rpt-003",
                reportTitle = "AirPods Gen 2 (case putih)",
                claimantId = "user-lain-04",
                claimantName = "Dewi Lestari",
                claimantEmail = "dewi.lestari@student.umn.ac.id",
                answer = "Case ada goresan kecil di sudut kanan dan stiker nama di dalamnya.",
                status = ClaimStatus.DISETUJUI,
                createdAt = now - 2 * day,
                decidedAt = now - day,
                decidedBy = staff.fullName,
                pickupCode = "K7M2QP"
            )
        )
    }

    /**
     * Percakapan contoh (dummy).
     *
     * Isinya meniru alur nyata: penemu barang menghubungi pemilik, lalu
     * menyepakati penitipan ke pos. Waktu dibuat relatif terhadap "sekarang"
     * supaya label waktu relatif (mis. "8 menit lalu") selalu masuk akal.
     */
    fun conversations(): List<ConversationThread> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        return listOf(
            ConversationThread(
                id = "chat-01",
                partnerName = "Sinta Maharani",
                partnerRole = "Penemu barang",
                reportId = "rpt-003",
                reportTitle = "AirPods Gen 2 (case putih)",
                lastMessage = "Baik, nanti saya serahkan ke Pos Basement ya.",
                lastMessageAt = now - 8 * minute,
                unreadCount = 2
            ),
            ConversationThread(
                id = "chat-02",
                partnerName = "Pak Rahmat",
                partnerRole = "Petugas Pos Basement",
                reportId = "rpt-001",
                reportTitle = "Tumbler Hitam Tumbler Angsa",
                lastMessage = "Barangnya sudah di pos, bawa KTM saat mengambil.",
                lastMessageAt = now - 3 * hour
            ),
            ConversationThread(
                id = "chat-03",
                partnerName = "Dewi Lestari",
                partnerRole = "Pemilik barang",
                reportId = "rpt-004",
                reportTitle = "Dompet Cokelat Berisi KTM",
                lastMessage = "Terima kasih banyak sudah menemukan dompet saya!",
                lastMessageAt = now - 2 * day
            )
        )
    }

    /** Isi pesan contoh untuk setiap percakapan di atas. */
    fun messages(): List<ChatMessage> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        return listOf(
            ChatMessage(
                id = "msg-01",
                threadId = "chat-01",
                text = "Hai, saya menemukan AirPods putih di meja perpustakaan lantai 4. Apakah ini milikmu?",
                fromMe = false,
                sentAt = now - 52 * minute
            ),
            ChatMessage(
                id = "msg-02",
                threadId = "chat-01",
                text = "Iya betul, case putih. Terima kasih sudah menemukannya!",
                fromMe = true,
                sentAt = now - 40 * minute
            ),
            ChatMessage(
                id = "msg-03",
                threadId = "chat-01",
                text = "Saya masih di sekitar perpustakaan sampai jam 3 sore.",
                fromMe = false,
                sentAt = now - 26 * minute
            ),
            ChatMessage(
                id = "msg-04",
                threadId = "chat-01",
                text = "Kalau boleh, titipkan ke Pos Basement saja biar aman.",
                fromMe = true,
                sentAt = now - 15 * minute
            ),
            ChatMessage(
                id = "msg-05",
                threadId = "chat-01",
                text = "Baik, nanti saya serahkan ke Pos Basement ya.",
                fromMe = false,
                sentAt = now - 8 * minute
            ),
            ChatMessage(
                id = "msg-06",
                threadId = "chat-02",
                text = "Selamat sore, tumbler hitam Anda sudah kami terima di pos.",
                fromMe = false,
                sentAt = now - 5 * hour
            ),
            ChatMessage(
                id = "msg-07",
                threadId = "chat-02",
                text = "Wah cepat sekali. Saya ambil besok pagi ya, Pak.",
                fromMe = true,
                sentAt = now - 4 * hour
            ),
            ChatMessage(
                id = "msg-08",
                threadId = "chat-02",
                text = "Barangnya sudah di pos, bawa KTM saat mengambil.",
                fromMe = false,
                sentAt = now - 3 * hour
            ),
            ChatMessage(
                id = "msg-09",
                threadId = "chat-03",
                text = "Dompet saya ketemu di kantin, benar itu dompet Anda?",
                fromMe = true,
                sentAt = now - 2 * day - 20 * minute
            ),
            ChatMessage(
                id = "msg-10",
                threadId = "chat-03",
                text = "Ya benar, ada KTM dan kartu ATM di dalamnya.",
                fromMe = false,
                sentAt = now - 2 * day - 10 * minute
            ),
            ChatMessage(
                id = "msg-11",
                threadId = "chat-03",
                text = "Terima kasih banyak sudah menemukan dompet saya!",
                fromMe = false,
                sentAt = now - 2 * day
            )
        )
    }
}
