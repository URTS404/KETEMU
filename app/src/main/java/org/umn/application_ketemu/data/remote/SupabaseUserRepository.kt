package org.umn.application_ketemu.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.UserRepository

/**
 * [UserRepository] di atas tabel `profiles` Supabase.
 *
 * Hak aksesnya dijaga dua lapis: aplikasi hanya menampilkan aksi admin kepada
 * peran ADMIN, dan RLS `profiles_admin_all` menolak UPDATE dari peran lain.
 * Jadi walaupun tombolnya dipaksa muncul, server tetap menolak.
 */
class SupabaseUserRepository(
    private val client: SupabaseClient
) : UserRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = MutableStateFlow<List<UserProfile>>(emptyList())

    init {
        scope.launch {
            while (isActive) {
                runCatching { load() }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun observeUsers(): StateFlow<List<UserProfile>> = cache.asStateFlow()

    override suspend fun refresh() {
        load()
    }

    override suspend fun setRole(userId: String, role: UserRole): UserProfile? {
        client.postgrest
            .from(TABLE)
            .update({ set("role", role.name) }) { filter { eq("id", userId) } }
        return reloadAndFind(userId)
    }

    override suspend fun setVerified(userId: String, verified: Boolean): UserProfile? {
        client.postgrest
            .from(TABLE)
            .update({ set("is_verified", verified) }) { filter { eq("id", userId) } }
        return reloadAndFind(userId)
    }

    override suspend fun setBanned(userId: String, banned: Boolean): UserProfile? {
        client.postgrest
            .from(TABLE)
            .update({ set("is_banned", banned) }) { filter { eq("id", userId) } }
        return reloadAndFind(userId)
    }

    private suspend fun reloadAndFind(userId: String): UserProfile? {
        load()
        return cache.value.firstOrNull { it.id == userId }
    }

    private suspend fun load() {
        cache.value = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(SELECT_COLUMNS)) { order("full_name", Order.ASCENDING) }
            .decodeList<ProfileDto>()
            .mapNotNull { it.toProfile() }
    }

    private fun ProfileDto.toProfile(): UserProfile? {
        val resolvedEmail = email ?: return null
        return UserProfile(
            id = id,
            fullName = fullName ?: resolvedEmail.substringBefore('@'),
            email = resolvedEmail,
            nimNidn = nimNidn.orEmpty(),
            phone = phone.orEmpty(),
            role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.USER),
            isVerified = isVerified,
            isBanned = isBanned
        )
    }

    private companion object {
        const val TABLE = "profiles"
        const val REFRESH_INTERVAL_MS = 20_000L
        const val SELECT_COLUMNS =
            "id,email,full_name,nim_nidn,phone,role,is_verified,is_banned"
    }
}
