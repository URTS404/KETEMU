package org.umn.application_ketemu.ui.feature.auth

import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.AuthErrorReason
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.AuthResult

/**
 * Implementasi [AuthRepository] untuk pengujian JVM (tanpa jaringan).
 *
 * Meniru perilaku Supabase Auth dengan data di memori agar ViewModel tetap bisa
 * diuji tanpa emulator dan tanpa koneksi.
 */
class FakeAuthRepository : AuthRepository {

    private val users = mutableMapOf<String, String>()

    var networkError = false
    var emailTaken = false

    fun addUser(email: String, password: String) {
        users[email.trim().lowercase()] = password
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        if (networkError) return AuthResult.Failure(AuthErrorReason.NETWORK)
        val normalized = email.trim().lowercase()
        val stored = users[normalized]
        return if (stored == null || stored != password) {
            AuthResult.Failure(AuthErrorReason.INVALID_CREDENTIALS)
        } else {
            AuthResult.Success(profileFor(normalized))
        }
    }

    override suspend fun signUp(
        fullName: String,
        email: String,
        nimNidn: String,
        phone: String,
        password: String
    ): AuthResult {
        if (networkError) return AuthResult.Failure(AuthErrorReason.NETWORK)
        val normalized = email.trim().lowercase()
        if (emailTaken || users.containsKey(normalized)) {
            return AuthResult.Failure(AuthErrorReason.USER_ALREADY_EXISTS)
        }
        users[normalized] = password
        return AuthResult.Success(profileFor(normalized, fullName))
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        if (networkError) return AuthResult.Failure(AuthErrorReason.NETWORK)
        return AuthResult.Success(null)
    }

    override suspend fun signOut() = Unit

    private fun profileFor(email: String, fullName: String? = null): UserProfile = UserProfile(
        id = "user-" + email.substringBefore('@'),
        fullName = fullName ?: email.substringBefore('@')
            .replace('.', ' ')
            .replaceFirstChar { it.uppercase() },
        email = email,
        nimNidn = "",
        role = UserRole.USER,
        isVerified = true
    )
}
