package org.umn.application_ketemu.data

import android.content.Context
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.auth.LocalAuthRepository
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.repository.InMemoryConversationRepository
import org.umn.application_ketemu.data.repository.InMemoryReportRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.ConversationRepository
import org.umn.application_ketemu.domain.repository.ReportRepository

/**
 * Wadah dependensi aplikasi (pengganti sementara Dependency Injection).
 *
 * Pada fase M0-Backend akan diubah memakai Hilt (@HiltAndroidApp + @Module),
 * sehingga kelas ini tidak lagi diperlukan. Struktur ketergantungannya sudah
 * disusun sama seperti rencana versi Hilt agar perpindahannya murni mekanis.
 */
class AppContainer(context: Context) {

    val reportRepository: ReportRepository = InMemoryReportRepository(DummyData.reports())

    /** Data percakapan (dummy) - sumber layar Pesan & layar Percakapan. */
    val conversationRepository: ConversationRepository = InMemoryConversationRepository(
        initialThreads = DummyData.conversations(),
        initialMessages = DummyData.messages()
    )

    val networkMonitor: NetworkMonitor = NetworkMonitor(context)

    /**
     * Autentikasi lokal (tanpa jaringan).
     *
     * Fase UTS sengaja belum memakai backend: Supabase baru diminta pada fase
     * berikutnya, jadi login diperiksa terhadap akun dummy aplikasi. Kelas
     * `SupabaseAuthRepository` tetap disimpan sebagai rujukan migrasi, bukan
     * dipakai di sini.
     */
    val authRepository: AuthRepository = LocalAuthRepository()

    val session: SessionStore = SessionStore()
}
