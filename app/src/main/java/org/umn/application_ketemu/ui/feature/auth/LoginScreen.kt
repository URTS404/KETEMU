package org.umn.application_ketemu.ui.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
 * Halaman Login — pintu masuk closed-ecosystem kampus (PRD bagian Autentikasi).
 *
 * Aturan yang terlihat pengguna:
 *  - hanya email kampus yang diterima (domain berasal dari local.properties
 *    lewat BuildConfig, jadi bisa diganti tanpa mengubah kode),
 *  - kata sandi minimal 6 karakter,
 *  - pesan kesalahan tampil tepat di bawah kolom, bukan popup, agar pengguna
 *    lanjut usia tidak kehilangan konteks.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandHeader()

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text(stringResource(R.string.login_email_label)) },
                placeholder = {
                    Text(stringResource(R.string.login_email_hint))
                },
                isError = uiState.error == LoginError.EMAIL_EMPTY ||
                    uiState.error == LoginError.NOT_CAMPUS_EMAIL,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text(stringResource(R.string.login_password_label)) },
                isError = uiState.error == LoginError.PASSWORD_TOO_SHORT,
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

            uiState.error?.let { error ->
                Text(
                    text = errorMessage(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = viewModel::onSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit
            ) {
                Text(
                    text = stringResource(R.string.login_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            InfoCard()

            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.login_register_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onRegisterClick) {
                    Text(
                        text = stringResource(R.string.login_register_action),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Identitas visual sementara: penanda kotak Teal berhuruf "K".
 * Akan diganti memakai berkas logo resmi pada tahap pengerjaan aset ikon.
 */
@Composable
private fun BrandHeader() {
    Surface(
        modifier = Modifier.size(88.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "K",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    Text(
        text = stringResource(R.string.login_title),
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = stringResource(R.string.login_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Catatan keterbatasan tahap ini, memakai warna aksen (Orange container). */
@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = stringResource(R.string.login_demo_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun errorMessage(error: LoginError): String = when (error) {
    LoginError.EMAIL_EMPTY -> stringResource(R.string.login_error_email_empty)
    LoginError.PASSWORD_TOO_SHORT -> stringResource(R.string.login_error_password)
    LoginError.NOT_CAMPUS_EMAIL -> stringResource(R.string.login_error_domain)
    LoginError.INVALID_CREDENTIALS -> stringResource(R.string.login_error_invalid_credentials)
    LoginError.RATE_LIMITED -> stringResource(R.string.login_error_rate_limited)
    LoginError.NETWORK -> stringResource(R.string.login_error_network)
}
