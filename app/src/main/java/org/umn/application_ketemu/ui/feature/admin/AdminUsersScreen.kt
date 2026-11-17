package org.umn.application_ketemu.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.ui.components.InitialsAvatar

/**
 * Daftar pengguna: dua wajah dalam satu layar.
 *
 * STAFF membuka layar ini dalam mode baca — mereka perlu tahu siapa saja yang
 * terdaftar (mis. memastikan pengklaim benar mahasiswa aktif), tetapi tidak
 * boleh mengubah peran siapa pun. ADMIN mendapat aksi ubah peran, verifikasi,
 * dan blokir. Pemisahan ini bukan sekadar kosmetik: RLS di Supabase juga
 * menolak UPDATE `profiles` dari peran selain admin, jadi tombol yang dipaksa
 * muncul tetap gagal di server.
 */
@Composable
fun AdminUsersScreen(
    viewModel: AdminUsersViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tones = rememberAdminTones()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                AdminReveal(index = 0) {
                    AdminScreenHeader(
                        title = stringResource(R.string.admin_menu_users),
                        subtitle = stringResource(R.string.admin_users_subtitle),
                        onBack = onBack
                    )
                }
            }

            item(key = "summary") {
                AdminReveal(index = 1) {
                    AdminSummaryCard(
                        label = stringResource(R.string.admin_users_summary_label),
                        count = state.accounts.size,
                        unit = stringResource(R.string.admin_users_summary_unit),
                        iconRes = R.drawable.ic_admin_users,
                        tone = tones.violet,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item(key = "section") {
                AdminReveal(index = 2) {
                    AdminSectionHeader(
                        title = stringResource(R.string.admin_users_section),
                        modifier = Modifier.padding(top = 8.dp),
                        trailing = {
                            Text(
                                // Sama dengan isi kartu ringkasan di atas agar
                                // angka tidak pernah berbeda antar keduanya.
                                text = stringResource(R.string.admin_users_count, state.accounts.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (state.accounts.isEmpty()) {
                item(key = "empty") {
                    AdminReveal(index = 3) { UsersEmptyCard() }
                }
            } else {
                itemsIndexed(items = state.accounts, key = { _, user -> user.id }) { index, user ->
                    // Nomor 0-2 sudah dipakai header, ringkasan, dan judul bagian.
                    AdminReveal(
                        index = index + 3,
                        modifier = Modifier.animateItem()
                    ) {
                        UserCard(
                            user = user,
                            canManage = state.canManage,
                            isSelf = user.id == state.currentUserId,
                            onRoleChange = { viewModel.changeRole(user.id, it) },
                            onToggleVerified = { viewModel.toggleVerified(user.id) },
                            onToggleBanned = { viewModel.toggleBanned(user.id) }
                        )
                    }
                }
            }

            item(key = "hint") {
                AdminReveal(index = state.accounts.size + 3) {
                    AdminHintCard(
                        title = stringResource(R.string.admin_users_hint_title),
                        text = stringResource(R.string.admin_users_desc),
                        tone = tones.violet,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Satu akun pada daftar pengguna.
 *
 * Identitas ditaruh di baris atas (avatar, nama, chip peran) dan email di baris
 * bawah yang dipisahkan garis tipis. Baris aksi hanya muncul untuk ADMIN; pada
 * mode baca kartu berhenti di email agar daftar tidak penuh tombol mati.
 */
@Composable
private fun UserCard(
    user: UserProfile,
    canManage: Boolean,
    isSelf: Boolean,
    onRoleChange: (UserRole) -> Unit,
    onToggleVerified: () -> Unit,
    onToggleBanned: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tones = rememberAdminTones()
    val roleTone = when (user.role) {
        UserRole.ADMIN -> tones.violet
        UserRole.STAFF -> tones.amber
        UserRole.USER -> tones.mint
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InitialsAvatar(
                    name = user.fullName,
                    size = 44.dp,
                    containerColor = roleTone.container,
                    contentColor = roleTone.content
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusChip(
                            text = adminRoleLabel(user.role),
                            container = roleTone.container,
                            content = roleTone.content
                        )
                        StatusChip(
                            text = if (user.isVerified) {
                                stringResource(R.string.admin_users_status_verified)
                            } else {
                                stringResource(R.string.admin_users_status_unverified)
                            },
                            container = MaterialTheme.colorScheme.surfaceContainerHigh,
                            content = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (user.isBanned) {
                            StatusChip(
                                text = stringResource(R.string.admin_users_status_banned),
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Garis mulai setelah avatar supaya sejajar dengan teks nama.
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canManage) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoleMenuButton(currentRole = user.role, onRoleChange = onRoleChange)
                    TextButton(onClick = onToggleVerified) {
                        Text(
                            text = if (user.isVerified) {
                                stringResource(R.string.admin_users_action_unverify)
                            } else {
                                stringResource(R.string.admin_users_action_verify)
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    TextButton(onClick = onToggleBanned, enabled = !isSelf) {
                        Text(
                            text = if (user.isBanned) {
                                stringResource(R.string.admin_users_action_unban)
                            } else {
                                stringResource(R.string.admin_users_action_ban)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelf) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Tombol "Ubah peran" dengan menu tiga pilihan; peran aktif ditandai centang. */
@Composable
private fun RoleMenuButton(
    currentRole: UserRole,
    onRoleChange: (UserRole) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = stringResource(R.string.admin_users_action_role),
                style = MaterialTheme.typography.labelMedium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UserRole.entries.forEach { role ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = adminRoleLabel(role),
                            fontWeight = if (role == currentRole) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        if (role != currentRole) onRoleChange(role)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color
) {
    Surface(shape = CircleShape, color = container) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

/**
 * Keadaan kosong di dalam daftar pengguna.
 *
 * Dipakai saat daftar akun belum terisi, supaya layar tidak berakhir dengan
 * judul bagian yang menggantung tanpa isi.
 */
@Composable
private fun UsersEmptyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_users_empty),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.admin_users_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
