package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole

/**
 * Manajemen akun untuk panel ADMIN.
 *
 * Sengaja dipisah dari [AuthRepository]: autentikasi mengurus "siapa saya",
 * sedangkan repositori ini mengurus "siapa boleh apa" terhadap akun orang lain.
 */
interface UserRepository {

    /** Daftar akun terbaru (dipakai layar Pengguna). */
    fun observeUsers(): StateFlow<List<UserProfile>>

    /** Muat ulang dari sumber data; aman dipanggil berulang. */
    suspend fun refresh()

    /** Ubah peran sebuah akun. Mengembalikan profil terbaru atau null bila gagal. */
    suspend fun setRole(userId: String, role: UserRole): UserProfile?

    /** Tandai akun sudah diverifikasi kampus (atau batalkan). */
    suspend fun setVerified(userId: String, verified: Boolean): UserProfile?

    /** Blokir / buka blokir akun. */
    suspend fun setBanned(userId: String, banned: Boolean): UserProfile?
}
