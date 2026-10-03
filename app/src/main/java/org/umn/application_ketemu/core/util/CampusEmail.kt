package org.umn.application_ketemu.core.util

/**
 * Mengecek apakah email memakai domain kampus (PRD: closed-ecosystem).
 *
 * Menerima domain utama (mis. `@umn.ac.id` untuk staf/dosen) maupun subdomain
 * (mis. `@student.umn.ac.id` untuk mahasiswa).
 */
fun isCampusEmail(email: String, campusDomain: String): Boolean {
    val domain = email.trim().substringAfterLast('@', "")
    return domain.equals(campusDomain, ignoreCase = true) ||
        domain.endsWith(".$campusDomain", ignoreCase = true)
}
