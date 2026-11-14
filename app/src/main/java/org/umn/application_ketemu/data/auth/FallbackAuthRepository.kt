package org.umn.application_ketemu.data.auth

import org.umn.application_ketemu.domain.repository.AuthErrorReason
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.AuthResult

/**
 * Autentikasi dua lapis: Supabase lebih dulu, akun demo lokal sebagai cadangan.
 *
 * Alasan: project harus tetap bisa didemokan di kelas walau jaringan kampus
 * sedang buruk atau kuota Supabase habis, sementara akun sungguhan tetap
 * memakai Supabase. Cadangan HANYA dipakai saat Supabase menolak kredensial
 * (bukan saat gangguan jaringan), supaya salah ketik sandi tidak diam-diam
 * "berhasil" lewat akun demo yang kebetulan emailnya sama.
 */
class FallbackAuthRepository(
    private val primary: AuthRepository,
    private val fallback: AuthRepository
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): AuthResult {
        val result = primary.signIn(email, password)
        return if (result is AuthResult.Failure && result.reason == AuthErrorReason.INVALID_CREDENTIALS) {
            fallback.signIn(email, password)
        } else {
            result
        }
    }

    /** Pendaftaran selalu ke sumber utama; akun demo bersifat tetap. */
    override suspend fun signUp(
        fullName: String,
        email: String,
        nimNidn: String,
        phone: String,
        password: String
    ): AuthResult = primary.signUp(fullName, email, nimNidn, phone, password)

    override suspend fun sendPasswordReset(email: String): AuthResult {
        val result = primary.sendPasswordReset(email)
        return if (result is AuthResult.Failure && result.reason == AuthErrorReason.NETWORK) {
            fallback.sendPasswordReset(email)
        } else {
            result
        }
    }

    override suspend fun signOut() {
        primary.signOut()
        fallback.signOut()
    }
}
