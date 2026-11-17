package org.umn.application_ketemu.ui.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.data.auth.TrialAccount

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
                shape = MaterialTheme.shapes.small,
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
                shape = MaterialTheme.shapes.small,
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

            // Akun uji coba: ditampilkan langsung di layar agar pengguna/penguji
            // tidak perlu membuka kode sumber untuk mengetahui email & sandinya.
            TrialAccountsCard(accounts = viewModel.trialAccounts)

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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ketemu_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(96.dp)
        )

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Daftar akun yang sudah terdaftar pada aplikasi, lengkap dengan kata sandinya.
 *
 * Ditampilkan sebagai kartu (bukan tombol) supaya tidak ada jalan pintas: siapa
 * pun tetap harus mengetik email + kata sandi pada form di atas untuk masuk.
 */
@Composable
private fun TrialAccountsCard(accounts: List<TrialAccount>) {
    if (accounts.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.login_trial_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = stringResource(R.string.login_trial_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            accounts.forEach { account ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${account.roleLabel} · ${account.fullName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.login_trial_email, account.email),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.login_trial_password, account.password),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
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
    LoginError.ACCOUNT_BLOCKED -> stringResource(R.string.login_error_blocked)
}
