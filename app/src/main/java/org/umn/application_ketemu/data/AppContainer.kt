package org.umn.application_ketemu.data

import android.content.Context
import org.umn.application_ketemu.core.network.NetworkMonitor
import org.umn.application_ketemu.data.auth.SupabaseAuthRepository
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.repository.InMemoryReportRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.repository.AuthRepository
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

    val networkMonitor: NetworkMonitor = NetworkMonitor(context)

    val authRepository: AuthRepository = SupabaseAuthRepository()

    val session: SessionStore = SessionStore()
}
