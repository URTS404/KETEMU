package org.umn.application_ketemu.data.auth

import com.russhwolf.settings.MapSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsCodeVerifierCache
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.umn.application_ketemu.BuildConfig
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.AuthErrorReason
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.AuthResult

/**
 * Implementasi [AuthRepository] memakai Supabase Auth (email + kata sandi).
 *
 * Kredensial dibaca dari BuildConfig yang bersumber dari local.properties
 * (tidak pernah di-commit). Client dibuat malas ([lazy]) sehingga aplikasi
 * tetap bisa dibuka tanpa memanggil jaringan sampai pengguna benar-benar
 * mencoba masuk/daftar.
 */
class SupabaseAuthRepository : AuthRepository {

    private val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                // Simpan sesi & cache PKCE di memori (prototipe). Menghindari kebutuhan
                // Settings platform (SharedPreferences) yang tidak tersedia otomatis.
                sessionManager = SettingsSessionManager(MapSettings())
                codeVerifierCache = SettingsCodeVerifierCache(MapSettings())
            }
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = try {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val user = client.auth.currentUserOrNull()?.toUserProfile()
        if (user == null) {
            AuthResult.Failure(AuthErrorReason.INVALID_CREDENTIALS)
        } else {
            AuthResult.Success(user)
        }
    } catch (e: Exception) {
        AuthResult.Failure(e.toAuthErrorReason())
    }

    override suspend fun signUp(
        fullName: String,
        email: String,
        nimNidn: String,
        phone: String,
        password: String
    ): AuthResult = try {
        val userInfo = client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName)
                put("nim_nidn", nimNidn)
                put("phone", phone)
            }
        }
        if (userInfo == null) {
            // Confirm email dinonaktifkan -> langsung masuk.
            AuthResult.Success(client.auth.currentUserOrNull()?.toUserProfile())
        } else {
            // Confirm email aktif -> pengguna menunggu verifikasi email.
            AuthResult.Success(null)
        }
    } catch (e: Exception) {
        AuthResult.Failure(e.toAuthErrorReason())
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = try {
        client.auth.resetPasswordForEmail(email = email)
        AuthResult.Success(null)
    } catch (e: Exception) {
        AuthResult.Failure(e.toAuthErrorReason())
    }

    override suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Best-effort; sesi lokal tetap dibersihkan oleh SessionStore.
        }
    }
}

/** Memetakan pengecualian Supabase/ktor menjadi alasan yang ramah pengguna. */
private fun Exception.toAuthErrorReason(): AuthErrorReason {
    val message = message.orEmpty()
    return when {
        message.contains("Invalid login credentials", ignoreCase = true) ->
            AuthErrorReason.INVALID_CREDENTIALS
        message.contains("User already registered", ignoreCase = true) ->
            AuthErrorReason.USER_ALREADY_EXISTS
        message.contains("rate limit", ignoreCase = true) ||
            message.contains("rate_limit", ignoreCase = true) ->
            AuthErrorReason.RATE_LIMITED
        else -> AuthErrorReason.NETWORK
    }
}

/** Memetakan [UserInfo] Supabase ke [UserProfile] yang dipakai seluruh aplikasi. */
private fun UserInfo.toUserProfile(): UserProfile {
    val metadata = userMetadata
    val fullName = metadata?.stringValue("full_name")
        ?: email?.substringBefore('@')?.toDisplayName()
        ?: "Pengguna Ketemu"
    return UserProfile(
        id = id,
        fullName = fullName,
        email = email.orEmpty(),
        nimNidn = metadata?.stringValue("nim_nidn").orEmpty(),
        phone = metadata?.stringValue("phone").orEmpty(),
        role = UserRole.USER,
        isVerified = true
    )
}

private fun Map<String, JsonElement>.stringValue(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

/** "budi.santoso" -> "Budi Santoso" agar sapaan terasa wajar. */
private fun String.toDisplayName(): String =
    split('.', '_', '-')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        .ifBlank { "Pengguna Ketemu" }
