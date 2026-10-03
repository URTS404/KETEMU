package org.umn.application_ketemu.domain.model

/**
 * Profil pengguna sesuai PRD: hanya civitas kampus (email kampus + NIM/NIDN).
 */
data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val nimNidn: String,
    val phone: String = "",
    val role: UserRole = UserRole.USER,
    val isVerified: Boolean = false,
    val photoUri: String? = null
) {
    /** Email harus memakai domain kampus, mis. @umn.ac.id (staf) atau @student.umn.ac.id (mahasiswa). */
    fun isCampusEmail(domain: String): Boolean {
        val emailDomain = email.substringAfterLast('@', "")
        return emailDomain.equals(domain, ignoreCase = true) ||
            emailDomain.endsWith(".$domain", ignoreCase = true)
    }
}
