package org.umn.application_ketemu.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole

/**
 * Penyimpan sesi LOKAL untuk tahap awal.
 *
 * Perannya:
 *  - Menahan pengguna di halaman Login sampai berhasil masuk (gerbang autentikasi).
 *  - Menyediakan peran aktif untuk membuktikan Acceptance Criteria #1 PRD:
 *    "User tidak bisa mengakses halaman Staff/Admin, dan sebaliknya."
 *
 * Validasi email kampus + panjang kata sandi dikerjakan di LoginViewModel.
 * Rencana penggantian (M2): Supabase Auth + penyimpanan sesi di DataStore,
 * sedangkan peran dibaca dari tabel `profiles` (bukan dipilih di UI).
 */
class SessionStore {

    private val _restored = kotlinx.coroutines.flow.MutableStateFlow(false)

    /**
     * `true` setelah aplikasi selesai memeriksa sesi Supabase yang tersimpan.
     * Gerbang memakainya agar layar Login tidak sempat muncul lalu berganti.
     */
    val restored: kotlinx.coroutines.flow.StateFlow<Boolean> = _restored

    fun markRestored() {
        _restored.value = true
    }

    /**
     * Profil akun yang BENAR-BENAR login (hasil [signIn]).
     *
     * Disimpan terpisah dari [currentUser] supaya sakelar peran demo hanya
     * mengubah FIELD `role`, tanpa menghapus/mengganti identitas akun asli.
     * Inilah perbaikan bug lama: "akun pertama hilang setelah pindah-pindah role".
     */
    private var authenticatedUser: UserProfile = DummyData.student

    private val _currentUser = MutableStateFlow(authenticatedUser)
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /** Membuka sesi memakai profil pengguna yang sudah terautentikasi. */
    fun signIn(user: UserProfile) {
        authenticatedUser = user
        _currentUser.value = user
        _isLoggedIn.value = true
    }

    fun signOut() {
        _isLoggedIn.value = false
    }

    /**
     * Ganti peran aktif — HANYA untuk keperluan demonstrasi di kelas.
     *
     * Yang diubah hanya `role` dari profil asli; id/nama/email/NIM/NIDN tetap
     * utuh. Kembali ke [UserRole.USER] akan memulihkan akun asli apa adanya,
     * bukan mengarang data dummy baru.
     */
    fun switchRole(role: UserRole) {
        _currentUser.value = authenticatedUser.copy(role = role)
    }
}

