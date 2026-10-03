package org.umn.application_ketemu.ui.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R

/**
 * Halaman Pendaftaran Akun (fitur Register).
 *
 * Validasi lokal (nama, email kampus, NIM, kata sandi) tetap dipertahankan.
 * Pendaftaran sesungguhnya lewat Supabase Auth; bila verifikasi email aktif,
 * pengguna diarahkan menunggu email konfirmasi.
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.action_back))
            }

            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.isAwaitingEmailConfirmation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.register_await_email_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.register_await_email_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.register_back_to_login),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                OutlinedTextField(
                    value = uiState.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(stringResource(R.string.register_name_label)) },
                    placeholder = { Text(stringResource(R.string.register_name_hint)) },
                    isError = uiState.error == RegisterError.NAME_EMPTY
                )

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(stringResource(R.string.login_email_label)) },
                    placeholder = { Text(stringResource(R.string.login_email_hint)) },
                    isError = uiState.error == RegisterError.EMAIL_EMPTY ||
                        uiState.error == RegisterError.NOT_CAMPUS_EMAIL ||
                        uiState.error == RegisterError.EMAIL_TAKEN,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextField(
                    value = uiState.nimNidn,
                    onValueChange = viewModel::onNimChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(stringResource(R.string.register_nim_label)) },
                    placeholder = { Text(stringResource(R.string.register_nim_hint)) },
                    isError = uiState.error == RegisterError.NIM_EMPTY,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = viewModel::onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(stringResource(R.string.register_phone_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(stringResource(R.string.register_password_label)) },
                    isError = uiState.error == RegisterError.PASSWORD_TOO_SHORT,
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = viewModel::onTogglePasswordVisibility) {
                            Text(
                                text = stringResource(
                                    if (uiState.isPasswordVisible) R.string.login_password_hide
                                    else R.string.login_password_show
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(stringResource(R.string.register_confirm_label)) },
                    isError = uiState.error == RegisterError.PASSWORD_MISMATCH,
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                uiState.error?.let { error ->
                    Text(
                        text = registerErrorMessage(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = viewModel::onRegister,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canSubmit
                ) {
                    Text(
                        text = stringResource(R.string.register_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun registerErrorMessage(error: RegisterError): String = when (error) {
    RegisterError.NAME_EMPTY -> stringResource(R.string.register_error_name)
    RegisterError.EMAIL_EMPTY -> stringResource(R.string.register_error_email)
    RegisterError.NOT_CAMPUS_EMAIL -> stringResource(R.string.register_error_domain)
    RegisterError.NIM_EMPTY -> stringResource(R.string.register_error_nim)
    RegisterError.PASSWORD_TOO_SHORT -> stringResource(R.string.register_error_password)
    RegisterError.PASSWORD_MISMATCH -> stringResource(R.string.register_error_mismatch)
    RegisterError.EMAIL_TAKEN -> stringResource(R.string.register_error_taken)
    RegisterError.RATE_LIMITED -> stringResource(R.string.register_error_rate_limited)
    RegisterError.NETWORK -> stringResource(R.string.register_error_network)
}

