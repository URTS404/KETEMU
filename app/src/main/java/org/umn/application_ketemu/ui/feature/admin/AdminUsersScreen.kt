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
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.umn.application_ketemu.R
import org.umn.application_ketemu.domain.model.UserProfile
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.ui.components.InitialsAvatar

/**
 * Daftar pengguna (read-only pada tahap 1).
 *
 * Layar ini menunjukkan kerangka "manajemen pengguna" tanpa berpura-pura punya
 * data server. Setelah Supabase aktif (tahap 2), isinya diganti tabel
 * `profiles` beserta aksi aktif/nonaktifkan akun.
 *
 * Susunannya mengikuti layar kerja admin lain: header besar, kartu ringkasan
 * bergradasi, lalu daftar kartu putih. Chip peran tetap dipertahankan supaya
 * STAFF dan ADMIN langsung terbedakan tanpa harus membaca email.
 *
 * Animasi: tiap bagian masuk bergiliran (memudar + naik halus) lewat
 * [AdminReveal], dan kartu akun memakai `Modifier.animateItem()` sehingga daftar
 * bergeser halus bila isinya berubah.
 */
@Composable
fun AdminUsersScreen(
    accounts: List<UserProfile>,
    onBack: () -> Unit
) {
    val tones = rememberAdminTones()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
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
                        count = accounts.size,
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
                                text = stringResource(R.string.admin_users_count, accounts.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (accounts.isEmpty()) {
                item(key = "empty") {
                    AdminReveal(index = 3) { UsersEmptyCard() }
                }
            } else {
                itemsIndexed(items = accounts, key = { _, user -> user.id }) { index, user ->
                    // Nomor 0-2 sudah dipakai header, ringkasan, dan judul bagian.
                    AdminReveal(
                        index = index + 3,
                        modifier = Modifier.animateItem()
                    ) {
                        UserCard(user = user)
                    }
                }
            }

            item(key = "hint") {
                AdminReveal(index = accounts.size + 3) {
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
 * bawah yang dipisahkan garis tipis. Pemisahan ini membuat email terbaca sebagai
 * data sekunder, bukan bagian dari nama.
 */
@Composable
private fun UserCard(
    user: UserProfile,
    modifier: Modifier = Modifier
) {
    val tones = rememberAdminTones()
    val roleTone = if (user.role == UserRole.ADMIN) tones.violet else tones.mint

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
                    containerColor = tones.mint.container,
                    contentColor = tones.mint.content
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = CircleShape,
                        color = roleTone.container
                    ) {
                        Text(
                            text = adminRoleLabel(user.role),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = roleTone.content
                        )
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
        }
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
