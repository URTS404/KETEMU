package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.umn.application_ketemu.domain.model.StaffKey
import org.umn.application_ketemu.domain.repository.StaffKeyRepository
import kotlin.random.Random

/**
 * Versi in-memory dari [StaffKeyRepository] untuk mode tanpa Supabase.
 *
 * Kunci dibuat acak dan hanya hidup selama proses aplikasi — cukup untuk
 * memperagakan alur "kunci staf" di kelas tanpa jaringan.
 */
class InMemoryStaffKeyRepository(
    initialKeys: List<StaffKey> = generate()
) : StaffKeyRepository {

    private val keys = MutableStateFlow(initialKeys)

    override fun observeKeys(): StateFlow<List<StaffKey>> = keys.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun regenerate(): Boolean {
        keys.value = generate()
        return true
    }

    override suspend fun redeem(code: String, targetUserId: String?): Boolean {
        val normalized = code.trim().uppercase()
        val target = keys.value.firstOrNull { it.code == normalized && !it.isUsed } ?: return false
        keys.value = keys.value.map {
            if (it.code == target.code) {
                it.copy(usedBy = targetUserId ?: "self", usedAt = System.currentTimeMillis())
            } else {
                it
            }
        }
        return true
    }

    private companion object {
        const val KUOTA = 10

        fun randomCode(): String {
            val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            fun block() = (1..4).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
            return "${block()}-${block()}"
        }

        fun generate(): List<StaffKey> = List(KUOTA) {
            StaffKey(code = randomCode(), createdAt = System.currentTimeMillis())
        }
    }
}
