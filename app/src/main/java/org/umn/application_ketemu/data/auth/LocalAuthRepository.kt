package org.umn.application_ketemu.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.AuthErrorReason
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.AuthResult

/** Satu akun lokal: profil kampus beserta kata sandinya (hanya di memori). */
data class LocalAccount(
    val profile: UserProfile,
    val password: String
)

/**
 * Baris kredensial yang ditampilkan di layar Login.
 *
 * Tujuannya agar penguji/pengguna tidak perlu membuka kode untuk mengetahui akun
 * mana yang bisa dipakai — cukup membaca kartu di layar login lalu menyalinnya.
 */
data class TrialAccount(
    val roleLabel: String,
    val fullName: String,
    val email: String,
    val password: String
)

/**
 * Autentikasi LOKAL (tanpa jaringan) untuk fase UI-first.
 *
 * Menggantikan pemakaian Supabase Auth pada `AppContainer`: kredensial diperiksa
 * terhadap akun yang memang sudah ada di aplikasi, sehingga login berjalan penuh
 * secara offline — tidak ada lagi tombol "Masuk sebagai Demo" yang melewati
 * pemeriksaan kata sandi.
 *
 * Aturan kata sandi: **sandi setiap akun = NIM/NIDN-nya**, mengikuti kebiasaan
 * akun kampus dan mudah diingat saat presentasi.
 *
 * Sumber akun:
 *  - [DummyData.student] (mahasiswa) dan [DummyData.staff] (satpam) — akun awal,
 *  - akun baru dari layar "Daftar" ditambahkan ke daftar yang sama, sehingga
 *    langsung dapat dipakai login pada sesi tersebut.
 *
 * Catatan fase berikutnya (M3): kelas ini diganti Supabase Auth + DataStore,
 * sementara [SupabaseAuthRepository] tetap disimpan sebagai rujukan migrasi.
 */
class LocalAuthRepository(
    initialAccounts: List<LocalAccount> = DEFAULT_ACCOUNTS
) : AuthRepository {

    private val accounts = MutableStateFlow(initialAccounts)

    override suspend fun signIn(email: String, password: String): AuthResult {
        val normalized = email.trim().lowercase()
        val account = accounts.value.firstOrNull { it.profile.email.lowercase() == normalized }
            ?: return AuthResult.Failure(AuthErrorReason.INVALID_CREDENTIALS)

        if (account.password != password) {
            return AuthResult.Failure(AuthErrorReason.INVALID_CREDENTIALS)
        }
        return AuthResult.Success(account.profile)
    }

    override suspend fun signUp(
        fullName: String,
        email: String,
        nimNidn: String,
        phone: String,
        password: String
    ): AuthResult {
        val normalized = email.trim().lowercase()
        val alreadyRegistered = accounts.value.any {
            it.profile.email.lowercase() == normalized
        }
        if (alreadyRegistered) {
            return AuthResult.Failure(AuthErrorReason.USER_ALREADY_EXISTS)
        }

        val profile = UserProfile(
            id = "user-" + normalized.substringBefore('@'),
            fullName = fullName.trim(),
            email = normalized,
            nimNidn = nimNidn.trim(),
            phone = phone.trim(),
            role = UserRole.USER,
            isVerified = true
        )
        accounts.value = accounts.value + LocalAccount(profile, password)
        return AuthResult.Success(profile)
    }

    /**
     * Simulasi lokal pengiriman tautan reset.
     *
     * Tidak ada email yang benar-benar dikirim pada fase UI-first; layar hanya
     * menampilkan konfirmasi bahwa permintaan sudah diproses.
     */
    override suspend fun sendPasswordReset(email: String): AuthResult = AuthResult.Success(null)

    override suspend fun signOut() = Unit

    /** Jumlah akun terdaftar — dipakai pengujian unit dan diagnosa. */
    fun accountCount(): Int = accounts.value.size

    companion object {

        /**
         * Akun awal aplikasi. Kata sandi sengaja disamakan dengan NIM/NIDN
         * sehingga tidak perlu data rahasia tambahan pada fase UI-first.
         */
        val DEFAULT_ACCOUNTS: List<LocalAccount> = listOf(
            LocalAccount(profile = DummyData.student, password = DummyData.student.nimNidn),
            LocalAccount(profile = DummyData.staff, password = DummyData.staff.nimNidn)
        )

        /** Versi ringkas [DEFAULT_ACCOUNTS] untuk ditampilkan di layar Login. */
        val TRIAL_ACCOUNTS: List<TrialAccount> = DEFAULT_ACCOUNTS.map { account ->
            TrialAccount(
                roleLabel = account.profile.role.roleLabel(),
                fullName = account.profile.fullName,
                email = account.profile.email,
                password = account.password
            )
        }

        private fun UserRole.roleLabel(): String = when (this) {
            UserRole.USER -> "Mahasiswa"
            UserRole.STAFF -> "Staf"
            UserRole.ADMIN -> "Admin"
        }
    }
}
