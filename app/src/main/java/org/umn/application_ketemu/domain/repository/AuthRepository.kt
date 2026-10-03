package org.umn.application_ketemu.domain.repository

import org.umn.application_ketemu.domain.model.UserProfile

/** Alasan kegagalan autentikasi (dipetakan dari error Supabase). */
enum class AuthErrorReason {
    INVALID_CREDENTIALS,
    USER_ALREADY_EXISTS,
    RATE_LIMITED,
    NETWORK
}

/** Hasil operasi autentikasi. */
sealed interface AuthResult {
    /**
     * Berhasil.
     *
     * [user] null hanya terjadi pada pendaftaran saat verifikasi email masih
     * diaktifkan di Supabase (pengguna belum otomatis masuk).
     */
    data class Success(val user: UserProfile?) : AuthResult
    data class Failure(val reason: AuthErrorReason) : AuthResult
}

/**
 * Kontrak autentikasi aplikasi.
 *
 * Layer tampilan hanya bergantung pada interface ini, sehingga implementasi
 * Supabase bisa diganti/diuji tanpa menyentuh ViewModel (pola sama dengan
 * [ReportRepository]).
 */
interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun signUp(
        fullName: String,
        email: String,
        nimNidn: String,
        phone: String,
        password: String
    ): AuthResult

    suspend fun sendPasswordReset(email: String): AuthResult

    suspend fun signOut()
}
