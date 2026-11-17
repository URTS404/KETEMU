package org.umn.application_ketemu.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R

/**
 * Dashboard moderator: ringkasan angka + pintu masuk ke tiap alat kerja.
 *
 * Angka diambil langsung dari repository (bukan dihitung di layar), sehingga
 * saat data pindah ke Room/Supabase nilainya otomatis ikut benar.
 *
 * Tampilan mengikuti referensi desain "ruang admin": header besar tanpa
 * TopAppBar, chip peran aktif, empat kartu ringkasan pastel, lalu daftar menu
 * berikon. Warna kartu berasal dari [rememberAdminTones] agar tetap terbaca
 * pada mode terang maupun gelap.
 */
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onBack: () -> Unit,
    onOpenModeration: () -> Unit,
    onOpenClaims: () -> Unit,
    onOpenAudit: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenKeys: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allowed by viewModel.isAllowed.collectAsStateWithLifecycle()
    val role by viewModel.role.collectAsStateWithLifecycle()

    // Pengguna tanpa hak tetap melihat kerangka panel + pesan penolakan.
    if (!allowed) {
        AdminScaffold(
            title = stringResource(R.string.admin_title),
            onBack = onBack
        ) { padding ->
            AdminAccessDenied(modifier = Modifier.padding(padding))
        }
        return
    }

    val tones = rememberAdminTones()
    val roleLabel = adminRoleLabel(role)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp)
        ) {
            AdminReveal(index = 0) {
                AdminScreenHeader(
                    title = stringResource(R.string.admin_title),
                    subtitle = stringResource(R.string.admin_subtitle),
                    roleLabel = roleLabel,
                    onBack = onBack
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            AdminReveal(index = 1) {
                AdminSectionHeader(
                    title = stringResource(R.string.admin_section_summary),
                    trailing = {
                        Text(
                            text = stringResource(R.string.admin_summary_scope),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            AdminReveal(index = 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        count = state.pendingModeration,
                        label = stringResource(R.string.admin_stat_moderation),
                        iconRes = R.drawable.ic_admin_hourglass,
                        tone = tones.mint,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        count = state.pendingClaims,
                        label = stringResource(R.string.admin_stat_claims),
                        iconRes = R.drawable.ic_admin_clipboard_check,
                        tone = tones.amber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AdminReveal(index = 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        count = state.archivedCount,
                        label = stringResource(R.string.admin_stat_archived),
                        iconRes = R.drawable.ic_admin_archive,
                        tone = tones.slate,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        count = state.totalReports,
                        label = stringResource(R.string.admin_stat_total),
                        iconRes = R.drawable.ic_admin_document,
                        tone = tones.blue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            AdminReveal(index = 4) {
                AdminSectionHeader(title = stringResource(R.string.admin_section_management))
            }
            Spacer(modifier = Modifier.height(12.dp))

            AdminReveal(index = 5) {
                AdminMenuRow(
                    title = stringResource(R.string.admin_menu_moderation),
                    subtitle = stringResource(R.string.admin_menu_moderation_desc),
                    iconRes = R.drawable.ic_admin_moderation,
                    tone = tones.mint,
                    onClick = onOpenModeration
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            AdminReveal(index = 6) {
                AdminMenuRow(
                    title = stringResource(R.string.admin_menu_claims),
                    subtitle = stringResource(R.string.admin_menu_claims_desc),
                    iconRes = R.drawable.ic_admin_clipboard_check,
                    tone = tones.amber,
                    onClick = onOpenClaims
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            AdminReveal(index = 7) {
                AdminMenuRow(
                    title = stringResource(R.string.admin_menu_audit),
                    subtitle = stringResource(R.string.admin_menu_audit_desc),
                    iconRes = R.drawable.ic_admin_history,
                    tone = tones.blue,
                    onClick = onOpenAudit
                )
            }
            // Menu Pengguna adalah wilayah ADMIN: STAFF bekerja pada barang,
            // ADMIN bekerja pada sistem (peran, verifikasi, blokir akun).
            if (role.canManageUsers) {
                Spacer(modifier = Modifier.height(10.dp))
                AdminReveal(index = 8) {
                    AdminMenuRow(
                        title = stringResource(R.string.admin_menu_users),
                        subtitle = stringResource(R.string.admin_menu_users_desc),
                        iconRes = R.drawable.ic_admin_users,
                        tone = tones.violet,
                        onClick = onOpenUsers
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                AdminReveal(index = 9) {
                    AdminMenuRow(
                        title = stringResource(R.string.admin_menu_keys),
                        subtitle = stringResource(R.string.admin_menu_keys_desc),
                        iconRes = R.drawable.ic_admin_users,
                        tone = tones.mint,
                        onClick = onOpenKeys
                    )
                }
            }

            // Aktivitas terakhir tetap dipertahankan sebagai kertas kerja cepat;
            // daftar lengkapnya ada pada menu "Log aktivitas".
            if (state.recentAudit.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                AdminReveal(index = 9) {
                    AdminSectionHeader(title = stringResource(R.string.admin_recent_audit))
                }
                Spacer(modifier = Modifier.height(12.dp))
                AdminReveal(index = 10) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            state.recentAudit.forEach { entry ->
                                AuditRow(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

