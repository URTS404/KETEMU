package org.umn.application_ketemu.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _currentUser = MutableStateFlow(DummyData.student)
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /** Membuka sesi memakai profil pengguna yang sudah terautentikasi. */
    fun signIn(user: UserProfile) {
        _currentUser.value = user
        _isLoggedIn.value = true
    }

    fun signOut() {
        _isLoggedIn.value = false
    }

    /** Ganti peran aktif — HANYA untuk keperluan demonstrasi di kelas. */
    fun switchRole(role: UserRole) {
        _currentUser.update { current ->
            when (role) {
                UserRole.USER -> DummyData.student.copy(email = current.email, fullName = current.fullName)
                UserRole.STAFF -> DummyData.staff
                UserRole.ADMIN -> DummyData.staff.copy(
                    id = "user-admin-01",
                    fullName = "Admin Ketemu",
                    email = "admin@umn.ac.id",
                    nimNidn = "ADMIN-001",
                    role = UserRole.ADMIN
                )
            }
        }
    }
}

