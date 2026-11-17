package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.umn.application_ketemu.domain.model.StaffKey

/**
 * Kunci staf (undangan sekali pakai) untuk naik peran menjadi STAFF.
 *
 * Mengapa lewat kunci, bukan tombol biasa: peran tidak boleh diubah pengguna
 * sendiri (RLS menolak UPDATE `profiles`), dan membuat akun auth dari aplikasi
 * akan membutuhkan `service_role` yang tidak boleh ada di APK. Kunci adalah
 * jalan tengah yang aman.
 */
interface StaffKeyRepository {

    /** Daftar kunci terbaru — hanya berisi data untuk ADMIN (dijaga RLS). */
    fun observeKeys(): StateFlow<List<StaffKey>>

    suspend fun refresh()

    /** Buat ulang kuota kunci (10 buah); kunci lama yang belum terpakai hangus. */
    suspend fun regenerate(): Boolean

    /**
     * Tebus kunci.
     *
     * @param targetUserId null = untuk diri sendiri; diisi = admin menaikkan akun lain.
     * @return true bila kunci sah, belum terpakai, dan peran sudah dinaikkan.
     */
    suspend fun redeem(code: String, targetUserId: String? = null): Boolean
}
