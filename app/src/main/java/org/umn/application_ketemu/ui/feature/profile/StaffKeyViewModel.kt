package org.umn.application_ketemu.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.StaffKeyRepository

/** Keadaan layar penebusan kunci staf. */
data class StaffKeyUiState(
    val currentRole: UserRole = UserRole.USER,
    val message: String? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false
)

/**
 * ViewModel penebusan kunci staf (jalur B: pengguna mendaftar biasa lalu
 * menebus kunci untuk menjadi STAFF).
 *
 * Perubahan peran dilakukan server; setelah berhasil, sesi lokal diperbarui
 * supaya panel admin langsung muncul tanpa perlu login ulang.
 */
class StaffKeyViewModel(
    private val staffKeyRepository: StaffKeyRepository,
    private val session: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StaffKeyUiState(currentRole = session.currentUser.value.role)
    )
    val uiState: StateFlow<StaffKeyUiState> = _uiState.asStateFlow()

    fun redeem(code: String) = viewModelScope.launch {
        val trimmed = code.trim()
        if (trimmed.length < 4) {
            _uiState.value = _uiState.value.copy(message = "Kode kunci terlalu pendek.")
            return@launch
        }
        _uiState.value = _uiState.value.copy(isSubmitting = true, message = null)
        val ok = runCatching { staffKeyRepository.redeem(trimmed) }.getOrDefault(false)

        if (ok) {
            val user = session.currentUser.value
            session.signIn(user.copy(role = UserRole.STAFF))
            _uiState.value = StaffKeyUiState(
                currentRole = UserRole.STAFF,
                isSuccess = true,
                message = "Berhasil. Peran Anda kini STAFF — panel moderasi sudah terbuka."
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                message = "Kunci tidak berlaku: mungkin salah ketik, sudah terpakai, atau bukan untuk email Anda."
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
