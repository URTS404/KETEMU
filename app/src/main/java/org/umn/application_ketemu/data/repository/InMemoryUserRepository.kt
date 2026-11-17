package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.UserRepository

/**
 * Versi in-memory dari [UserRepository], dipakai saat Supabase belum
 * dikonfigurasi (mode UTS / demo kelas tanpa jaringan).
 *
 * Semua aksi hanya mengubah daftar di memori — cukup untuk memperagakan alur
 * kerja ADMIN, tetapi hilang saat proses aplikasi berakhir.
 */
class InMemoryUserRepository(
    initialUsers: List<UserProfile> = emptyList()
) : UserRepository {

    private val users = MutableStateFlow(initialUsers.sortedBy { it.fullName })

    override fun observeUsers(): StateFlow<List<UserProfile>> = users.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun setRole(userId: String, role: UserRole): UserProfile? =
        update(userId) { it.copy(role = role) }

    override suspend fun setVerified(userId: String, verified: Boolean): UserProfile? =
        update(userId) { it.copy(isVerified = verified) }

    override suspend fun setBanned(userId: String, banned: Boolean): UserProfile? =
        update(userId) { it.copy(isBanned = banned) }

    private fun update(userId: String, transform: (UserProfile) -> UserProfile): UserProfile? {
        val current = users.value
        val target = current.firstOrNull { it.id == userId } ?: return null
        val updated = transform(target)
        users.value = current.map { if (it.id == userId) updated else it }
            .sortedBy { it.fullName }
        return updated
    }
}
