package org.umn.application_ketemu.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
 */
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onBack: () -> Unit,
    onOpenModeration: () -> Unit,
    onOpenClaims: () -> Unit,
    onOpenAudit: () -> Unit,
    onOpenUsers: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allowed by viewModel.isAllowed.collectAsStateWithLifecycle()

    AdminScaffold(
        title = stringResource(R.string.admin_title),
        onBack = onBack
    ) { padding ->
        if (!allowed) {
            AdminAccessDenied(modifier = Modifier.padding(padding))
            return@AdminScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard(
                    count = state.pendingModeration,
                    label = stringResource(R.string.admin_stat_moderation),
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    count = state.pendingClaims,
                    label = stringResource(R.string.admin_stat_claims),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard(
                    count = state.archivedCount,
                    label = stringResource(R.string.admin_stat_archived),
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    count = state.totalReports,
                    label = stringResource(R.string.admin_stat_total),
                    modifier = Modifier.weight(1f)
                )
            }

            AdminMenuRow(
                title = stringResource(R.string.admin_menu_moderation),
                subtitle = stringResource(R.string.admin_menu_moderation_desc),
                onClick = onOpenModeration
            )
            AdminMenuRow(
                title = stringResource(R.string.admin_menu_claims),
                subtitle = stringResource(R.string.admin_menu_claims_desc),
                onClick = onOpenClaims
            )
            AdminMenuRow(
                title = stringResource(R.string.admin_menu_audit),
                subtitle = stringResource(R.string.admin_menu_audit_desc),
                onClick = onOpenAudit
            )
            AdminMenuRow(
                title = stringResource(R.string.admin_menu_users),
                subtitle = stringResource(R.string.admin_menu_users_desc),
                onClick = onOpenUsers
            )

            if (state.recentAudit.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.admin_recent_audit),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                state.recentAudit.forEach { entry ->
                    AuditRow(entry = entry)
                }
            }
        }
    }
}
