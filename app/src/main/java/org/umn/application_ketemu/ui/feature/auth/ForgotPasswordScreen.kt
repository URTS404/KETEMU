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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R

/**
 * Halaman "Lupa Kata Sandi".
 *
 * Supabase mengirim tautan atur ulang ke email (tidak membedakan email
 * terdaftar/tidak demi mencegah enumerasi akun), jadi layar hanya meminta email
 * lalu menampilkan konfirmasi pengiriman.
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
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
                text = stringResource(R.string.forgot_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.forgot_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.isSuccess) {
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
                            text = stringResource(R.string.forgot_success_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.forgot_success_desc),
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
                        text = stringResource(R.string.forgot_back_to_login),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
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
                    isError = uiState.error == ForgotPasswordError.EMAIL_EMPTY ||
                        uiState.error == ForgotPasswordError.NOT_CAMPUS_EMAIL,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                uiState.error?.let { error ->
                    Text(
                        text = forgotErrorMessage(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = viewModel::onSendReset,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canSubmit
                ) {
                    Text(
                        text = stringResource(R.string.forgot_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun forgotErrorMessage(error: ForgotPasswordError): String = when (error) {
    ForgotPasswordError.EMAIL_EMPTY -> stringResource(R.string.forgot_error_email)
    ForgotPasswordError.NOT_CAMPUS_EMAIL -> stringResource(R.string.forgot_error_domain)
    ForgotPasswordError.RATE_LIMITED -> stringResource(R.string.forgot_error_rate_limited)
    ForgotPasswordError.NETWORK -> stringResource(R.string.forgot_error_network)
}

