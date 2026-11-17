package org.umn.application_ketemu.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO tabel `profiles`.
 *
 * Peran pengguna (USER/STAFF/ADMIN) adalah data milik database, bukan metadata
 * auth: admin bisa menaikkan peran tanpa mengubah akun, dan aplikasi selalu
 * membaca nilai terbaru dari sini saat login.
 *
 * Catatan: kolom opsional di database (`nim_nidn`, `phone`) WAJIB bertipe
 * nullable di sini — PostgREST mengirim `null`, dan kotlinx.serialization
 * menolak `null` untuk properti non-null meski punya nilai default.
 */
@Serializable
internal data class ProfileDto(
    val id: String = "",
    val email: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("nim_nidn") val nimNidn: String? = null,
    val phone: String? = null,
    val role: String = "USER",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_banned") val isBanned: Boolean = false
)
