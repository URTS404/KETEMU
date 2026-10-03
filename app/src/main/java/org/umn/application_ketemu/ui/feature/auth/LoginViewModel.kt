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

/**
 * Pesan kesalahan login. Dipakai sebagai id resource string (bukan teks langsung)
 * agar UI tetap bisa berbahasa Indonesia tanpa mengubah ViewModel.
 */
enum class LoginError {
    EMAIL_EMPTY,
    PASSWORD_TOO_SHORT,
    NOT_CAMPUS_EMAIL,
    INVALID_CREDENTIALS,
    RATE_LIMITED,
    NETWORK
}

/**
 * State layar Login.
 *
 * @param campusDomain domain email kampus, mis. "umn.ac.id" — berasal dari
 *   local.properties lewat BuildConfig, sehingga aturan closed-ecosystem PRD
 *   dapat diubah tanpa menyentuh kode.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val error: LoginError? = null,
    val isSubmitting: Boolean = false
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

/**
 * ViewModel Login.
 *
 * Validasi lokal (domain kampus + panjang kata sandi) tetap dipertahankan agar
 * umpan balik cepat dan bisa diuji tanpa jaringan. Pengecekan kredensial
 * sesungguhnya dilakukan Supabase Auth lewat [AuthRepository].
 */
class LoginViewModel(
    private val session: SessionStore,
    private val authRepository: AuthRepository,
    val campusDomain: String = BuildConfig.CAMPUS_EMAIL_DOMAIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /** Mengecek kredensial; bila lolos, sesi dibuka dan gerbang navigasi terbuka. */
    fun onSignIn() {
        val state = _uiState.value

        val error = when {
            state.email.isBlank() -> LoginError.EMAIL_EMPTY
            state.password.length < MIN_PASSWORD_LENGTH -> LoginError.PASSWORD_TOO_SHORT
            !isCampusEmail(state.email, campusDomain) ->
                LoginError.NOT_CAMPUS_EMAIL

            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.signIn(state.email, state.password)) {
                is AuthResult.Success -> {
                    val user = result.user
                    if (user != null) {
                        session.signIn(user)
                    } else {
                        _uiState.update {
                            it.copy(isSubmitting = false, error = LoginError.INVALID_CREDENTIALS)
                        }
                    }
                }

                is AuthResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.reason.toLoginError())
                }
            }
        }
    }

    private fun AuthErrorReason.toLoginError(): LoginError = when (this) {
        AuthErrorReason.INVALID_CREDENTIALS, AuthErrorReason.USER_ALREADY_EXISTS ->
            LoginError.INVALID_CREDENTIALS
        AuthErrorReason.RATE_LIMITED -> LoginError.RATE_LIMITED
        AuthErrorReason.NETWORK -> LoginError.NETWORK
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
