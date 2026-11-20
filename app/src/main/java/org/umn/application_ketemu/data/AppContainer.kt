package org.umn.application_ketemu.data

import org.umn.application_ketemu.data.repository.PendingChatStore

import kotlinx.coroutines.launch

import kotlinx.coroutines.SupervisorJob

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.CoroutineScope

import android.content.Context
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.core.notification.AndroidSystemNotifier
import org.umn.application_ketemu.core.notification.NotificationTapRequests
import org.umn.application_ketemu.core.notification.SharedPreferencesSystemNotificationSettings
import org.umn.application_ketemu.core.notification.SystemNotificationSettings
import org.umn.application_ketemu.core.notification.SystemNotifier
import org.umn.application_ketemu.data.auth.FallbackAuthRepository
import org.umn.application_ketemu.data.auth.LocalAuthRepository
import org.umn.application_ketemu.data.auth.SupabaseAuthRepository
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.remote.SupabaseAuditLogRepository
import org.umn.application_ketemu.data.remote.SupabaseClaimRepository
import org.umn.application_ketemu.data.remote.SupabaseMatchRepository
import org.umn.application_ketemu.data.remote.SupabaseNotificationRepository
import org.umn.application_ketemu.data.remote.SupabaseProvider
import org.umn.application_ketemu.data.local.KetemuDatabase
import org.umn.application_ketemu.data.repository.RoomReportRepository
import org.umn.application_ketemu.data.remote.SupabaseChatRepository
import org.umn.application_ketemu.data.remote.SupabaseStaffKeyRepository
import org.umn.application_ketemu.data.remote.SupabaseUserRepository
import org.umn.application_ketemu.data.repository.InMemoryAuditLogRepository
import org.umn.application_ketemu.data.repository.InMemoryClaimRepository
import org.umn.application_ketemu.data.repository.InMemoryConversationRepository
import org.umn.application_ketemu.data.repository.InMemoryMatchRepository
import org.umn.application_ketemu.data.repository.InMemoryNotificationRepository
import org.umn.application_ketemu.data.repository.InMemoryReportRepository
import org.umn.application_ketemu.data.repository.InMemoryChatRepository
import org.umn.application_ketemu.data.repository.InMemoryStaffKeyRepository
import org.umn.application_ketemu.data.repository.InMemoryUserRepository
import org.umn.application_ketemu.data.repository.SystemNotifyingNotificationRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.repository.AuditLogRepository
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.ClaimRepository
import org.umn.application_ketemu.domain.repository.ConversationRepository
import org.umn.application_ketemu.domain.repository.MatchRepository
import org.umn.application_ketemu.domain.repository.NotificationRepository
import org.umn.application_ketemu.domain.repository.ReportRepository
import org.umn.application_ketemu.domain.repository.ChatRepository
import org.umn.application_ketemu.domain.repository.StaffKeyRepository
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
     * Sesi pengguna aktif.
     *
     * Diletakkan paling atas karena dipakai dekorator notifikasi di bawah
     * (urutan inisialisasi properti Kotlin mengikuti urutan penulisan).
     */
    val session: SessionStore = SessionStore()

    /**
     * Sakelar "notifikasi luar aplikasi" yang diatur pengguna di halaman Akun.
     *
     * Bawaannya aktif; pilihan disimpan di SharedPreferences sehingga bertahan
     * setelah aplikasi ditutup. Dekorator notifikasi di bawah membaca sakelar ini.
     */
    val systemNotificationSettings: SystemNotificationSettings =
        SharedPreferencesSystemNotificationSettings(context)

    /**
     * Pengirim notifikasi panel sistem (di luar aplikasi).
     *
     * Kabar in-app tetap disimpan oleh [notificationRepository]; kelas ini
     * memunculkan salinannya di notification tray Android.
     */
    val systemNotifier: SystemNotifier = AndroidSystemNotifier(context)

    /**
     * Permintaan membuka kotak masuk dari ketukan kartu notifikasi panel sistem.
     * Diisi MainActivity, dikonsumsi KetemuNavHost.
     */
    val notificationTapRequests: NotificationTapRequests = NotificationTapRequests()

    /**
     * Pemantau koneksi.
     *
     * Diletakkan SEBELUM [reportRepository] karena repositori Room memakainya
     * untuk memutuskan kapan antrean tulis dikirim (urutan inisialisasi
     * properti Kotlin mengikuti urutan penulisan; memakai properti yang belum
     * diinisialisasi menghasilkan null/NPE).
     */
    val networkMonitor: NetworkMonitor = NetworkMonitor(context)

    /**
     * Sumber data laporan.
     *
     * Supabase aktif -> Room menjadi sumber data UI, Supabase mengisi cache dan
     * menerima perubahan lewat antrean `outbox` (lihat [RoomReportRepository]).
     * Tanpa kunci -> data contoh in-memory seperti pada tahap UTS. Panel admin,
     * layar detail, dan laporan saya memakai interface yang sama sehingga tidak
     * perlu diubah.
     */
    val reportRepository: ReportRepository = if (SupabaseProvider.isConfigured) {
        RoomReportRepository(
            client = SupabaseProvider.client,
            db = KetemuDatabase.get(context),
            networkMonitor = networkMonitor
        )
    } else {
        InMemoryReportRepository(DummyData.reports())
    }

    /**
     * Pasangan laporan hilang <-> temuan (Tahap 2).
     *
     * Pencocokan dihitung server (`db/0018_auto_match.sql`): trigger mencatat
     * pasangan sekaligus mengirim notifikasi POSSIBLE_MATCH. Aplikasi hanya
     * membaca hasilnya untuk bagian "Mungkin barangmu" di halaman detail.
     * Tanpa Supabase, pasangan contoh dari [DummyData.matches] dipakai agar
     * bagian tersebut tetap bisa didemokan.
     */
    val matchRepository: MatchRepository = if (SupabaseProvider.isConfigured) {
        SupabaseMatchRepository(SupabaseProvider.client)
    } else {
        InMemoryMatchRepository(DummyData.matches())
    }

    /** Data percakapan (dummy) - sumber layar Pesan & layar Percakapan. */
    val conversationRepository: ConversationRepository = InMemoryConversationRepository(
        initialThreads = DummyData.conversations(),
        initialMessages = DummyData.messages()
    )

    /**
     * Kotak masuk notifikasi (lonceng Beranda + layar Notifikasi).
     *
     * Tanpa Supabase, notifikasi contoh disemai agar badge langsung terlihat
     * saat demo. Dengan Supabase, baris dibaca dari tabel `notifications`
     * (db/0016) dan RLS menentukan milik siapa notifikasi itu.
     *
     * Dibungkus [SystemNotifyingNotificationRepository] supaya kabar baru juga
     * tampil di panel sistem Android (di luar aplikasi), dengan aturan yang sama
     * seperti kotak masuk: hanya milik pengguna aktif dan belum dibaca.
     */
    val notificationRepository: NotificationRepository = SystemNotifyingNotificationRepository(
        delegate = if (SupabaseProvider.isConfigured) {
            SupabaseNotificationRepository(SupabaseProvider.client)
        } else {
            InMemoryNotificationRepository(DummyData.notifications())
        },
        session = session,
        notifier = systemNotifier,
        settings = systemNotificationSettings
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

    /** Percakapan per laporan (db/0019). Realtime hanya saat layar chat terbuka. */
    private val pendingChatStore = PendingChatStore(context)

    val chatRepository: ChatRepository = if (SupabaseProvider.isConfigured) {
        SupabaseChatRepository(
            SupabaseProvider.client,
            pendingChatStore,
            java.io.File(context.filesDir, "chat_outbox")
        )
    } else {
        InMemoryChatRepository()
    }

    /** Kunci staf (undangan sekali pakai) — dibaca penuh hanya oleh ADMIN. */
    val staffKeyRepository: StaffKeyRepository = if (SupabaseProvider.isConfigured) {
        SupabaseStaffKeyRepository(SupabaseProvider.client)
    } else {
        InMemoryStaffKeyRepository()
    }

    /**
     * Autentikasi.
     *
     * Bila `local.properties` berisi SUPABASE_URL + kunci publishable, aplikasi
     * memakai Supabase Auth sungguhan; akun demo lokal tetap tersedia sebagai
     * cadangan agar demo kelas tidak bergantung jaringan. Tanpa kunci, seluruh
     * autentikasi berjalan lokal (mode UTS).
     */
    val authRepository: AuthRepository = if (SupabaseProvider.isConfigured) {
        // Supabase aktif = login WAJIB lewat Supabase. Cadangan lokal hanya
        // dipakai bila Supabase memang tidak dikonfigurasi; kalau tidak, login
        // akan tampak "berhasil" tetapi tanpa JWT sehingga semua data hilang.
        SupabaseAuthRepository(SupabaseProvider.client)
    } else {
        LocalAuthRepository()
    }

    /** Apakah project Supabase sudah dikonfigurasi pada build ini. */
    /**
     * Begitu jaringan kembali, pesan yang tertahan di outbox dikirim ulang
     * otomatis — tanpa pengguna harus membuka layar percakapan.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Pulihkan sesi Supabase yang tersimpan agar tidak perlu login ulang.
        appScope.launch {
            runCatching {
                val profil = (authRepository as? SupabaseAuthRepository)?.restoreSession()
                if (profil != null) {
                    session.signIn(profil)
                    android.util.Log.i("Auth", "sesi dipulihkan: " + profil.email)
                    // Antrean hanya bisa dikirim setelah sesi tersedia; tanpa ini
                    // pesan/gambar tertunda menunggu sampai jaringan berubah.
                    runCatching { chatRepository.flushPending() }
                    android.util.Log.i("Chat", "antrean dikirim setelah sesi dipulihkan")
                }
            }.onFailure {
                android.util.Log.w("Auth", "pemulihan sesi gagal: " + it.message)
            }
            session.markRestored()
        }

        appScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) runCatching { chatRepository.flushPending() }
            }
        }
    }

    fun supabaseConfigured(): Boolean = SupabaseProvider.isConfigured
}
