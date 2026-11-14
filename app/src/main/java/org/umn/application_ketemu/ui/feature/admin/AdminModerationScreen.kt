package org.umn.application_ketemu.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.components.ReportStatusChip

/**
 * Antrean moderasi: laporan baru yang belum diverifikasi petugas.
 *
 * Aksi utama meneruskan laporan ke Pos Barang Temuan (status berikutnya pada
 * timeline), aksi kedua mengarsipkan laporan yang tidak relevan.
 */
@Composable
fun AdminModerationScreen(
    viewModel: AdminModerationViewModel,
    onBack: () -> Unit
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val allowed by viewModel.isAllowed.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.clearMessage()
    }

    AdminScaffold(
        title = stringResource(R.string.admin_menu_moderation),
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        if (!allowed) {
            AdminAccessDenied(modifier = Modifier.padding(padding))
            return@AdminScaffold
        }

        if (queue.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.admin_queue_empty),
                description = stringResource(R.string.admin_queue_empty_desc),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            return@AdminScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = queue, key = { it.id }) { report ->
                ModerationCard(
                    report = report,
                    onVerify = { viewModel.verifyAndForward(report.id) },
                    onArchive = { viewModel.archive(report.id) }
                )
            }
        }
    }
}

@Composable
private fun ModerationCard(
    report: Report,
    onVerify: () -> Unit,
    onArchive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${report.ownerName} · ${report.lastSeenLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = RelativeTime.format(report.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ReportStatusChip(status = report.status)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onVerify,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.admin_action_verify))
                }
                OutlinedButton(onClick = onArchive) {
                    Text(text = stringResource(R.string.admin_action_archive))
                }
            }
        }
    }
}
