package org.umn.application_ketemu.ui.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.StaffKeyRepository
import org.umn.application_ketemu.domain.repository.UserRepository

/**
 * Data yang ditampilkan layar Pengguna.
 *
 * [canManage] memisahkan dua wajah layar yang sama: STAFF hanya membaca daftar,
 * ADMIN mendapat aksi ubah peran, verifikasi, dan blokir.
 */
data class AdminUsersUiState(
    val accounts: List<UserProfile> = emptyList(),
    val canManage: Boolean = false,
    val currentUserId: String? = null,
    val message: String? = null
)

/**
 * ViewModel layar Pengguna.
 *
 * Keputusan hak akses diambil dari peran yang tersimpan di sesi, bukan dari
 * siapa yang membuka layar: kalau peran berubah (mis. admin menurunkan dirinya
 * sendiri), aksi langsung tertutup pada emisi berikutnya.
 */
class AdminUsersViewModel(
    private val userRepository: UserRepository,
    private val staffKeyRepository: StaffKeyRepository,
    private val session: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminUsersUiState(
            canManage = session.currentUser.value.role.canManageUsers,
            currentUserId = session.currentUser.value.id
        )
    )
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.observeUsers().collect { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    canManage = session.currentUser.value.role.canManageUsers,
                    currentUserId = session.currentUser.value.id
                )
            }
        }
        viewModelScope.launch { runCatching { userRepository.refresh() } }
    }

    /**
     * Ubah peran akun sasaran; hanya ADMIN yang boleh.
     *
     * Kenaikan menjadi STAFF **wajib** lewat kunci (jalur C): itulah yang
     * menjaga kuota 10 tetap berlaku, sehingga admin pun tidak bisa menambah
     * staf tanpa batas. Penurunan peran tidak memerlukan kunci.
     */
    fun changeRole(userId: String, role: UserRole) = manage {
        if (role == UserRole.STAFF) {
            _uiState.value = _uiState.value.copy(
                message = "Pakai kunci staf untuk mengangkat akun menjadi STAFF."
            )
            return@manage
        }
        val updated = userRepository.setRole(userId, role)
        messageFor(updated) { "Peran ${it.fullName} diubah menjadi ${roleLabel(it.role)}." }
    }

    /**
     * Naikkan akun menjadi STAFF dengan menebus salah satu kunci (jalur C).
     *
     * Server yang memutuskan kunci sah atau tidak; aplikasi hanya meneruskan.
     */
    fun promoteWithKey(userId: String, code: String) = manage {
        val ok = staffKeyRepository.redeem(code, userId)
        val target = _uiState.value.accounts.firstOrNull { it.id == userId }
        _uiState.value = _uiState.value.copy(
            message = when {
                !ok -> "Kunci tidak berlaku, sudah terpakai, atau bukan untuk akun ini."
                target != null -> "${target.fullName} kini STAFF. Kunci sudah hangus."
                else -> "Peran berhasil dinaikkan menjadi STAFF."
            }
        )
    }

    fun toggleVerified(userId: String) = manage {
        val target = _uiState.value.accounts.firstOrNull { it.id == userId }
        val updated = userRepository.setVerified(userId, target?.isVerified != true)
        messageFor(updated) {
            if (it.isVerified) "${it.fullName} ditandai terverifikasi kampus."
            else "Verifikasi ${it.fullName} dicabut."
        }
    }

    fun toggleBanned(userId: String) = manage {
        if (userId == _uiState.value.currentUserId) {
            _uiState.value = _uiState.value.copy(message = "Anda tidak dapat memblokir akun sendiri.")
            return@manage
        }
        val target = _uiState.value.accounts.firstOrNull { it.id == userId }
        val updated = userRepository.setBanned(userId, target?.isBanned != true)
        messageFor(updated) {
            if (it.isBanned) "${it.fullName} diblokir dan tidak dapat masuk."
            else "Blokir ${it.fullName} dibuka."
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun manage(block: suspend () -> Unit) = viewModelScope.launch {
        if (!session.currentUser.value.role.canManageUsers) {
            _uiState.value = _uiState.value.copy(message = AdminUiText.ACCESS_DENIED)
            return@launch
        }
        try {
            block()
        } catch (e: Exception) {
            android.util.Log.e("AdminUsers", "aksi pengguna gagal: ${e.message}", e)
            _uiState.value = _uiState.value.copy(message = AdminUiText.ACTION_FAILED)
        }
    }

    private fun messageFor(updated: UserProfile?, build: (UserProfile) -> String) {
        _uiState.value = _uiState.value.copy(
            message = if (updated == null) AdminUiText.USER_NOT_FOUND else build(updated)
        )
    }

    /**
     * Label peran versi non-Compose.
     *
     * `adminRoleLabel()` di AdminCommon.kt adalah @Composable sehingga tidak
     * bisa dipanggil dari ViewModel; pesan snackbar butuh teks biasa.
     */
    private fun roleLabel(role: UserRole): String = when (role) {
        UserRole.USER -> "USER"
        UserRole.STAFF -> "STAFF"
        UserRole.ADMIN -> "ADMIN"
    }
}
