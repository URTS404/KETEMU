package org.umn.application_ketemu.domain.model

/**
 * Satu kunci staf (undangan sekali pakai).
 *
 * Kunci dibuat ADMIN, dipakai **sekali**, lalu hangus. Kunci yang belum dipakai
 * tidak boleh dibaca pengguna biasa — RLS `staff_keys_admin_read` di database
 * menegakkan itu, jadi daftar ini hanya terisi untuk peran ADMIN.
 *
 * @param reservedEmail bila diisi, kunci hanya berlaku untuk email tersebut
 *   (jalur A: admin menyiapkan akun sejak awal).
 */
data class StaffKey(
    val code: String,
    val reservedEmail: String? = null,
    val note: String? = null,
    val usedBy: String? = null,
    val usedAt: Long? = null,
    val createdAt: Long = 0L
) {
    val isUsed: Boolean get() = usedBy != null
}
