package org.umn.application_ketemu.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.components.LoadingSkeletonList
import org.umn.application_ketemu.ui.components.ReportCard
import org.umn.application_ketemu.ui.components.ReportLocationSection
import org.umn.application_ketemu.ui.components.StatusChipRow
import org.umn.application_ketemu.ui.components.StatusTimeline

/** Halaman detail barang + Dashboard Tracking (timeline status) sesuai PRD. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    viewModel: ReportDetailViewModel,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()

    // Prosedur pengambilan dijelaskan di depan supaya tidak ada kejutan di pos:
    // barang hanya diserahkan setelah KTM dicocokkan oleh staff.
    var showClaimProcedure by remember { mutableStateOf(false) }

    if (showClaimProcedure) {
        ClaimProcedureDialog(onDismiss = { showClaimProcedure = false })
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            UiState.Loading -> LoadingSkeletonList(
                modifier = Modifier.padding(innerPadding),
                itemCount = 2
            )

            UiState.Empty, is UiState.Error -> EmptyState(
                title = stringResource(R.string.empty_reports_title),
                description = stringResource(R.string.empty_reports_desc),
                modifier = Modifier.padding(innerPadding)
            )

            is UiState.Success -> DetailContent(
                report = state.data,
                candidates = candidates,
                onClaimClick = { showClaimProcedure = true },
                onOpenReport = onOpenReport,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/** Penjelasan langkah pengambilan barang di pos (pencegahan salah klaim). */
@Composable
private fun ClaimProcedureDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.claim_info_title)) },
        text = { Text(stringResource(R.string.claim_info_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_understood))
            }
        }
    )
}

@Composable
private fun DetailContent(
    report: Report,
    candidates: List<Report>,
    onClaimClick: () -> Unit,
    onOpenReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ReportPhoto(report)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        if (report.type == ItemType.LOST) R.string.filter_lost
                        else R.string.filter_found
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (report.type == ItemType.LOST) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                StatusChipRow(status = report.status, syncStatus = report.syncStatus)
            }

            Text(text = report.title, style = MaterialTheme.typography.headlineSmall)
            Text(text = report.description, style = MaterialTheme.typography.bodyLarge)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = report.pos?.let { "${it.name} · ${it.building}" }
                        ?: report.lastSeenLocation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Titik GPS hanya muncul bila laporan memang menyimpannya, sehingga
            // laporan lama (tanpa koordinat) tetap tampil seperti sebelumnya.
            report.coordinates?.takeIf { it.isValid }?.let { point ->
                ReportLocationSection(
                    point = point,
                    label = report.pos?.name ?: report.lastSeenLocation,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Dilaporkan oleh ${report.ownerName} · " +
                    RelativeTime.format(report.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StatusTimeline(report = report, modifier = Modifier.fillMaxWidth())

            if (candidates.isNotEmpty()) {
                MatchSection(
                    report = report,
                    candidates = candidates,
                    onOpenReport = onOpenReport
                )
            }

            if (report.status == ReportStatus.DISERAHKAN_KE_POS ||
                report.status == ReportStatus.SIAP_DIAMBIL
            ) {
                Button(
                    onClick = { onClaimClick() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.action_claim),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Bagian "Mungkin barangmu": pasangan yang ditemukan server (db/0018) untuk
 * laporan ini. Kartu kandidat membuka detail laporan seberang; tombol klaim
 * sengaja belum ada karena alur klaim menyusul pada Tahap 4.
 */
@Composable
private fun MatchSection(
    report: Report,
    candidates: List<Report>,
    onOpenReport: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(
                if (report.type == ItemType.LOST) R.string.match_section_title_lost
                else R.string.match_section_title_found
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.match_section_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        candidates.forEach { candidate ->
            ReportCard(
                report = candidate,
                onClick = { onOpenReport(candidate.id) }
            )
        }
    }
}

@Composable
private fun ReportPhoto(report: Report) {
    if (report.photoUri.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.report_photo_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AsyncImage(
            model = report.photoUri,
            contentDescription = report.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
        )
    }
}
