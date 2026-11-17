package org.umn.application_ketemu.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.domain.model.StaffKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daftar kunci staf (undangan sekali pakai) — hanya untuk ADMIN.
 *
 * Kunci tampil dalam huruf monospace supaya mudah dibacakan lewat telepon, dan
 * satu ketukan menyalinnya ke papan klip. Tombol "Buat 10 kunci baru" memanggil
 * fungsi server; kunci lama yang belum terpakai hangus agar kuota tetap 10.
 */
@Composable
fun AdminKeysScreen(
    viewModel: AdminKeysViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tones = rememberAdminTones()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    AdminScaffold(
        title = stringResource(R.string.admin_keys_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "summary") {
                AdminReveal(index = 0) {
                    AdminSummaryCard(
                        label = stringResource(R.string.admin_keys_summary_label),
                        count = state.keys.count { !it.isUsed },
                        unit = stringResource(R.string.admin_keys_summary_unit),
                        iconRes = R.drawable.ic_admin_users,
                        tone = tones.violet
                    )
                }
            }

            item(key = "action") {
                AdminReveal(index = 1) {
                    Button(
                        onClick = viewModel::regenerate,
                        enabled = state.canManage && !state.isWorking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.admin_keys_regenerate))
                    }
                }
            }

            item(key = "section") {
                AdminReveal(index = 2) {
                    AdminSectionHeader(
                        title = stringResource(R.string.admin_keys_section),
                        modifier = Modifier.padding(top = 8.dp),
                        trailing = {
                            Text(
                                text = stringResource(R.string.admin_keys_count, state.keys.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (state.keys.isEmpty()) {
                item(key = "empty") {
                    AdminReveal(index = 3) {
                        AdminHintCard(
                            title = stringResource(R.string.admin_keys_empty),
                            text = stringResource(R.string.admin_keys_empty_desc),
                            tone = tones.violet
                        )
                    }
                }
            } else {
                itemsIndexed(items = state.keys, key = { _, key -> key.code }) { index, key ->
                    AdminReveal(index = index + 3, modifier = Modifier.animateItem()) {
                        StaffKeyCard(
                            key = key,
                            onCopy = {
                                clipboard.setText(AnnotatedString(key.code))
                                viewModel.notify("Kunci ${key.code} disalin.")
                            }
                        )
                    }
                }
            }

            item(key = "hint") {
                AdminReveal(index = state.keys.size + 3) {
                    AdminHintCard(
                        title = stringResource(R.string.admin_keys_hint_title),
                        text = stringResource(R.string.admin_keys_hint),
                        tone = tones.mint,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffKeyCard(
    key: StaffKey,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tones = rememberAdminTones()
    val usedTone = MaterialTheme.colorScheme.surfaceContainerHigh
    val formatter = remember { SimpleDateFormat("d MMM yyyy HH:mm", Locale("id", "ID")) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = key.code,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = CircleShape,
                    color = if (key.isUsed) usedTone else tones.mint.container
                ) {
                    Text(
                        text = if (key.isUsed) {
                            stringResource(R.string.admin_keys_status_used)
                        } else {
                            stringResource(R.string.admin_keys_status_free)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (key.isUsed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            tones.mint.content
                        }
                    )
                }
            }

            Text(
                text = buildString {
                    append(
                        if (key.isUsed) {
                            stringResource(R.string.admin_keys_used_at, formatter.format(Date(key.usedAt ?: 0L)))
                        } else {
                            stringResource(R.string.admin_keys_created_at, formatter.format(Date(key.createdAt)))
                        }
                    )
                    key.reservedEmail?.let {
                        append(" · ")
                        append(stringResource(R.string.admin_keys_reserved, it))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!key.isUsed) {
                TextButton(onClick = onCopy) {
                    Text(stringResource(R.string.admin_keys_copy), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
