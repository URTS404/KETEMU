package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.umn.application_ketemu.core.util.PickupCode
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.ClaimStatus
import org.umn.application_ketemu.domain.model.HandoverResult
import org.umn.application_ketemu.domain.repository.ClaimRepository

/**
 * Penyimpanan klaim sementara di memori.
 *
 * Perilakunya sengaja dibuat sama dengan versi Room/Supabase nanti:
 *  - keputusan bersifat idempoten,
 *  - kode pengambilan dibuat hanya saat disetujui,
 *  - pembacaan berupa Flow (reaktif).
 */
class InMemoryClaimRepository(
    initialClaims: List<Claim> = emptyList()
) : ClaimRepository {

    private val claims = MutableStateFlow(initialClaims)

    override fun observeClaims(): Flow<List<Claim>> =
        claims.map { list -> list.sortedByDescending { it.createdAt } }

    override fun observePendingClaims(): Flow<List<Claim>> =
        observeClaims().map { list -> list.filter { it.status == ClaimStatus.MENUNGGU } }

    override suspend fun getClaim(id: String): Claim? =
        claims.value.firstOrNull { it.id == id }

    override suspend fun submit(claim: Claim) {
        claims.value = claims.value + claim
    }

    override suspend fun decide(
        claimId: String,
        approve: Boolean,
        decidedBy: String,
        note: String?
    ): Claim? {
        val existing = claims.value.firstOrNull { it.id == claimId } ?: return null
        if (existing.status != ClaimStatus.MENUNGGU) return existing

        val updated = existing.copy(
            status = if (approve) ClaimStatus.DISETUJUI else ClaimStatus.DITOLAK,
            decidedAt = System.currentTimeMillis(),
            decidedBy = decidedBy,
            decisionNote = note,
            pickupCode = if (approve) PickupCode.generate(claimId) else null
        )
        claims.value = claims.value.map { if (it.id == claimId) updated else it }
        return updated
    }

    /**
     * Versi demo: hanya menandai waktu serah terima di memori. Status laporan
     * tetap diubah pemanggil ([AdminClaimsViewModel]) karena repositori demo ini
     * tidak memegang daftar laporan.
     */
    override suspend fun completeHandover(claimId: String, code: String?): HandoverResult {
        val existing = claims.value.firstOrNull { it.id == claimId }
            ?: return HandoverResult(berhasil = false)
        if (existing.status != ClaimStatus.DISETUJUI) return HandoverResult(berhasil = false)
        val kode = code?.trim().orEmpty()
        if (kode.isNotEmpty() && !kode.equals(existing.pickupCode, ignoreCase = true)) {
            return HandoverResult(berhasil = false)
        }

        val updated = existing.copy(handedOverAt = System.currentTimeMillis())
        claims.value = claims.value.map { if (it.id == claimId) updated else it }
        // Demo tidak punya fungsi database, jadi audit dicatat pemanggil.
        return HandoverResult(berhasil = true, auditDiServer = false)
    }
}
