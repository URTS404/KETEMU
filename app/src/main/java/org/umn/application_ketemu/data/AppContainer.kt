package org.umn.application_ketemu.data

import android.content.Context
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.auth.FallbackAuthRepository
import org.umn.application_ketemu.data.auth.LocalAuthRepository
import org.umn.application_ketemu.data.auth.SupabaseAuthRepository
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.remote.SupabaseAuditLogRepository
import org.umn.application_ketemu.data.remote.SupabaseClaimRepository
import org.umn.application_ketemu.data.remote.SupabaseProvider
import org.umn.application_ketemu.data.remote.SupabaseReportRepository
import org.umn.application_ketemu.data.remote.SupabaseUserRepository
import org.umn.application_ketemu.data.repository.InMemoryAuditLogRepository
import org.umn.application_ketemu.data.repository.InMemoryClaimRepository
import org.umn.application_ketemu.data.repository.InMemoryConversationRepository
import org.umn.application_ketemu.data.repository.InMemoryReportRepository
import org.umn.application_ketemu.data.repository.InMemoryUserRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.repository.AuditLogRepository
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.ClaimRepository
import org.umn.application_ketemu.domain.repository.ConversationRepository
import org.umn.application_ketemu.domain.repository.ReportRepository
import org.umn.application_ketemu.domain.repository.UserRepository

/**
 * Wadah dependensi aplikasi (pengganti sementara Dependency Injection).
 *
 * Pada fase M0-Backend akan diubah memakai Hilt (@HiltAndroidApp + @Module),
 * sehingga kelas ini tidak lagi diperlukan. Struktur ketergantungannya sudah
 * disusun sama seperti rencana versi Hilt agar perpindahannya murni mekanis.
 */
class AppContainer(context: Context) {

    /**
     * Sumber data laporan.
     *
     * Supabase aktif -> data dibaca dari tabel `reports` (lihat
     * [SupabaseReportRepository]); tanpa kunci -> data contoh in-memory seperti
     * pada tahap UTS. Panel admin, layar detail, dan laporan saya memakai
     * interface yang sama sehingga tidak perlu diubah.
     */
    val reportRepository: ReportRepository = if (SupabaseProvider.isConfigured) {
        SupabaseReportRepository(SupabaseProvider.client)
    } else {
        InMemoryReportRepository(DummyData.reports())
    }

    /** Data percakapan (dummy) - sumber layar Pesan & layar Percakapan. */
    val conversationRepository: ConversationRepository = InMemoryConversationRepository(
        initialThreads = DummyData.conversations(),
        initialMessages = DummyData.messages()
    )

    /** Pengajuan klaim + antrean keputusan moderator. */
    val claimRepository: ClaimRepository = if (SupabaseProvider.isConfigured) {
        SupabaseClaimRepository(SupabaseProvider.client)
    } else {
        InMemoryClaimRepository(DummyData.claims())
    }

    /** Log aktivitas moderator (append-only). */
    val auditLogRepository: AuditLogRepository = if (SupabaseProvider.isConfigured) {
        SupabaseAuditLogRepository(SupabaseProvider.client)
    } else {
        InMemoryAuditLogRepository()
    }

    /**
     * Manajemen akun (khusus ADMIN): ubah peran, verifikasi, blokir.
     *
     * Tanpa Supabase, daftar akun diisi akun contoh supaya alur kerja admin
     * tetap bisa diperagakan di kelas.
     */
    val userRepository: UserRepository = if (SupabaseProvider.isConfigured) {
        SupabaseUserRepository(SupabaseProvider.client)
    } else {
        InMemoryUserRepository(listOf(DummyData.student, DummyData.staff))
    }

    val networkMonitor: NetworkMonitor = NetworkMonitor(context)

    /**
     * Autentikasi.
     *
     * Bila `local.properties` berisi SUPABASE_URL + kunci publishable, aplikasi
     * memakai Supabase Auth sungguhan; akun demo lokal tetap tersedia sebagai
     * cadangan agar demo kelas tidak bergantung jaringan. Tanpa kunci, seluruh
     * autentikasi berjalan lokal (mode UTS).
     */
    val authRepository: AuthRepository = if (SupabaseProvider.isConfigured) {
        FallbackAuthRepository(
            primary = SupabaseAuthRepository(SupabaseProvider.client),
            fallback = LocalAuthRepository()
        )
    } else {
        LocalAuthRepository()
    }

    val session: SessionStore = SessionStore()

    /** Apakah project Supabase sudah dikonfigurasi pada build ini. */
    fun supabaseConfigured(): Boolean = SupabaseProvider.isConfigured
}
