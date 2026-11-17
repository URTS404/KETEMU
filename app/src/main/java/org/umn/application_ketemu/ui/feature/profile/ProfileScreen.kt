package org.umn.application_ketemu.ui.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.notification.SystemNotificationAccess
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.ui.components.InitialsAvatar

/**
 * Layar Profil.
 *
 * Selain menampilkan identitas civitas (email kampus + NIM/NIDN), di sini
 * disediakan sakelar peran untuk KEPERLUAN DEMO, guna membuktikan Acceptance
 * Criteria #1 PRD:
 * "User tidak bisa mengakses halaman Staff/Admin, dan sebaliknya."
 *
 * Sakelar ini akan dihapus pada M2 dan digantikan oleh sesi Supabase Auth.
 *
 * Halaman ini juga memuat [SystemNotificationsSetting], pengaturan perangkat
 * yang benar-benar dipakai aplikasi (bukan simulasi): pengguna bisa mematikan
 * kartu notifikasi di panel sistem tanpa kehilangan kotak masuk in-app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserProfile,
    systemNotificationsEnabled: Boolean,
    onSystemNotificationsEnabledChange: (Boolean) -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenStaffKey: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_profile)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InitialsAvatar(
                            name = user.fullName,
                            size = 84.dp,
                            textStyle = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "NIM/NIDN: ${user.nimNidn}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Peran aktif:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = user.role.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = if (user.isVerified) "Status: Terverifikasi kampus"
                        else "Status: Belum diverifikasi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(R.string.profile_settings_title),
                style = MaterialTheme.typography.labelLarge
            )

            SystemNotificationsSetting(
                enabled = systemNotificationsEnabled,
                onEnabledChange = onSystemNotificationsEnabledChange
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.profile_role_simulation_title),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(R.string.profile_role_simulation_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.entries.forEach { role ->
                    FilterChip(
                        selected = user.role == role,
                        onClick = { onSwitchRole(role) },
                        label = { Text(role.name) }
                    )
                }
            }

            // Pintu masuk panel moderasi: hanya dirender untuk STAFF/ADMIN.
            // Lapis kedua ada di ViewModel (menolak aksi bila peran berubah).
            if (user.role.canModerate) {
                Button(
                    onClick = onOpenAdmin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.admin_open),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    text = stringResource(R.string.admin_open_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Jalur B: pengguna biasa yang memegang kunci dari pengelola Pos.
            if (!user.role.canModerate) {
                OutlinedButton(
                    onClick = onOpenStaffKey,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.profile_staff_key_action),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    text = stringResource(R.string.profile_staff_key_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.profile_logout),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Sakelar "Notifikasi luar aplikasi" beserta penjelasan statusnya.
 *
 * Tiga keadaan yang dibedakan:
 *  - Izin POST_NOTIFICATIONS belum diberikan (Android 13+): menyalakan sakelar
 *    langsung memunculkan dialog izin, dan hasilnya menentukan posisi sakelar.
 *  - Izin ada tetapi notifikasi aplikasi diblokir dari Pengaturan Android
 *    (kanal "Kabar Ketemu" dimatikan): sakelar tetap menyala, ditambah petunjuk
 *    dan tombol menuju Pengaturan karena aplikasi tidak bisa membukanya sendiri.
 *  - Semua beres: kartu Ketemu muncul di panel notifikasi.
 *
 * Kotak masuk in-app tidak pernah terpengaruh — kabar tetap tersimpan di
 * lonceng Beranda meskipun notifikasi luar dimatikan.
 */
@Composable
private fun SystemNotificationsSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // Status sistem dibaca ulang saat layar kembali aktif: pengguna bisa
    // mengubah izin/kanal dari Pengaturan Android lalu kembali ke aplikasi.
    var systemAllows by remember {
        mutableStateOf(SystemNotificationAccess.canPost(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        systemAllows = SystemNotificationAccess.canPost(context)
        // Ditolak berarti kartu memang tidak akan tampil; sakelar dikembalikan
        // ke posisi mati supaya tidak menjanjikan sesuatu yang mustahil.
        onEnabledChange(granted)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        systemAllows = SystemNotificationAccess.canPost(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Satu target sentuh untuk seluruh baris; TalkBack membacanya
                    // sebagai sakelar karena `role = Role.Switch`.
                    .toggleable(
                        value = enabled,
                        role = Role.Switch,
                        onValueChange = { wanted ->
                            when {
                                !wanted -> onEnabledChange(false)
                                SystemNotificationAccess.hasPermission(context) -> {
                                    onEnabledChange(true)
                                    systemAllows = SystemNotificationAccess.canPost(context)
                                }
                                else -> permissionLauncher.launch(
                                    SystemNotificationAccess.PERMISSION_POST_NOTIFICATIONS
                                )
                            }
                        }
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_settings_notifications),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.profile_settings_notifications_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // onCheckedChange = null: penanganan ketukan ada di baris (toggleable).
                Switch(checked = enabled, onCheckedChange = null)
            }
        }

        if (enabled && !systemAllows) {
            Text(
                text = stringResource(R.string.profile_settings_notifications_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = { SystemNotificationAccess.openSettings(context) }) {
                Text(
                    text = stringResource(R.string.profile_settings_notifications_open),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Text(
                text = stringResource(R.string.profile_settings_notifications_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


