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
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.repository.AuthErrorReason
import org.umn.application_ketemu.domain.repository.AuthRepository
import org.umn.application_ketemu.domain.repository.AuthResult

/** Pesan kesalahan pendaftaran akun. */
enum class RegisterError {
    NAME_EMPTY,
    EMAIL_EMPTY,
    NOT_CAMPUS_EMAIL,
    NIM_EMPTY,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    EMAIL_TAKEN,
    RATE_LIMITED,
    NETWORK
}

/** State layar Pendaftaran. */
data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val nimNidn: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val error: RegisterError? = null,
    val isSubmitting: Boolean = false,
    val isAwaitingEmailConfirmation: Boolean = false
) {
    val canSubmit: Boolean
        get() = fullName.isNotBlank() && email.isNotBlank() && nimNidn.isNotBlank() &&
            password.isNotBlank() && confirmPassword.isNotBlank() && !isSubmitting
}

/**
 * ViewModel Pendaftaran akun.
 *
 * Validasi lokal (nama, email kampus, NIM, panjang/kecocokan kata sandi) tetap
 * dipertahankan. Pendaftaran sesungguhnya dilakukan Supabase Auth lewat
 * [AuthRepository]. Bila verifikasi email masih aktif di Supabase, pengguna
 * diarahkan menunggu email konfirmasi alih-alih langsung masuk.
 */
class RegisterViewModel(
    private val session: SessionStore,
    private val authRepository: AuthRepository,
    val campusDomain: String = BuildConfig.CAMPUS_EMAIL_DOMAIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value, error = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onNimChange(value: String) = _uiState.update { it.copy(nimNidn = value, error = null) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, error = null) }

    fun onTogglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onRegister() {
        val state = _uiState.value

        val error = when {
            state.fullName.isBlank() -> RegisterError.NAME_EMPTY
            state.email.isBlank() -> RegisterError.EMAIL_EMPTY
            !isCampusEmail(state.email, campusDomain) ->
                RegisterError.NOT_CAMPUS_EMAIL
            state.nimNidn.isBlank() -> RegisterError.NIM_EMPTY
            state.password.length < MIN_PASSWORD_LENGTH -> RegisterError.PASSWORD_TOO_SHORT
            state.password != state.confirmPassword -> RegisterError.PASSWORD_MISMATCH
            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.signUp(
                fullName = state.fullName,
                email = state.email,
                nimNidn = state.nimNidn,
                phone = state.phone,
                password = state.password
            )) {
                is AuthResult.Success -> {
                    val user = result.user
                    if (user != null) {
                        session.signIn(user)
                    } else {
                        _uiState.update {
                            it.copy(isSubmitting = false, isAwaitingEmailConfirmation = true)
                        }
                    }
                }

                is AuthResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.reason.toRegisterError())
                }
            }
        }
    }

    private fun AuthErrorReason.toRegisterError(): RegisterError = when (this) {
        AuthErrorReason.USER_ALREADY_EXISTS -> RegisterError.EMAIL_TAKEN
        AuthErrorReason.RATE_LIMITED -> RegisterError.RATE_LIMITED
        AuthErrorReason.INVALID_CREDENTIALS, AuthErrorReason.NETWORK -> RegisterError.NETWORK
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
