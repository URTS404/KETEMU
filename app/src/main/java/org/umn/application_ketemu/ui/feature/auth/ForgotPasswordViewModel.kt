package org.umn.application_ketemu.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.umn.application_ketemu.BuildConfig
import org.umn.application_ketemu.core.util.isCampusEmail
import org.umn.application_ketemu.domain.repository.AuthErrorReason
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.AuthResult

/** Pesan kesalahan fitur "Lupa Kata Sandi". */
enum class ForgotPasswordError {
    EMAIL_EMPTY,
    NOT_CAMPUS_EMAIL,
    RATE_LIMITED,
    NETWORK
}

/** State layar "Lupa Kata Sandi". */
data class ForgotPasswordUiState(
    val email: String = "",
    val error: ForgotPasswordError? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && !isSubmitting
}

/**
 * ViewModel "Lupa Kata Sandi".
 *
 * Supabase tidak membedakan email terdaftar/tidak saat mengirim tautan reset
 * (untuk mencegah enumerasi akun), jadi layar hanya menampilkan konfirmasi
 * "tautan terkirim" setelah permintaan berhasil dikirim.
 */
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
    val campusDomain: String = BuildConfig.CAMPUS_EMAIL_DOMAIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onSendReset() {
        val state = _uiState.value

        val error = when {
            state.email.isBlank() -> ForgotPasswordError.EMAIL_EMPTY
            !isCampusEmail(state.email, campusDomain) ->
                ForgotPasswordError.NOT_CAMPUS_EMAIL
            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordReset(state.email)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, isSuccess = true)
                }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.reason.toForgotPasswordError())
                }
            }
        }
    }

    private fun AuthErrorReason.toForgotPasswordError(): ForgotPasswordError = when (this) {
        AuthErrorReason.RATE_LIMITED -> ForgotPasswordError.RATE_LIMITED
        else -> ForgotPasswordError.NETWORK
    }
}
