package org.umn.application_ketemu.ui.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.StaffKey
import org.umn.application_ketemu.domain.repository.StaffKeyRepository

/** Data layar "Kunci staf" pada panel admin. */
data class AdminKeysUiState(
    val keys: List<StaffKey> = emptyList(),
    val canManage: Boolean = false,
    val message: String? = null,
    val isWorking: Boolean = false
)

/**
 * ViewModel daftar kunci staf.
 *
 * Kuota kunci dijaga di server (`generate_staff_keys` menghapus kunci lama yang
 * belum terpakai lalu membuat 10 baru), jadi layar ini hanya menampilkan dan
 * memicu pembuatan ulang.
 */
class AdminKeysViewModel(
    private val staffKeyRepository: StaffKeyRepository,
    private val session: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminKeysUiState(canManage = session.currentUser.value.role.canManageUsers)
    )
    val uiState: StateFlow<AdminKeysUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            staffKeyRepository.observeKeys().collect { keys ->
                _uiState.value = _uiState.value.copy(
                    keys = keys.sortedWith(compareBy({ it.isUsed }, { it.code })),
                    canManage = session.currentUser.value.role.canManageUsers
                )
            }
        }
        viewModelScope.launch { runCatching { staffKeyRepository.refresh() } }
    }

    fun regenerate() = viewModelScope.launch {
        if (!session.currentUser.value.role.canManageUsers) {
            _uiState.value = _uiState.value.copy(message = AdminUiText.ACCESS_DENIED)
            return@launch
        }
        _uiState.value = _uiState.value.copy(isWorking = true)
        val ok = runCatching { staffKeyRepository.regenerate() }.getOrDefault(false)
        _uiState.value = _uiState.value.copy(
            isWorking = false,
            message = if (ok) "10 kunci baru dibuat. Kunci lama yang belum terpakai hangus."
            else AdminUiText.ACTION_FAILED
        )
    }

    fun notify(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
