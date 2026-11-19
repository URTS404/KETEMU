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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.ClaimStatus
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report
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
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val myClaim by viewModel.myClaim.collectAsStateWithLifecycle()
    val canClaim by viewModel.canClaim.collectAsStateWithLifecycle()
    val canChat by viewModel.canChat.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Formulir klaim: pemilik menuliskan ciri barang, lalu petugas pos yang
    // memverifikasi. Prosedur pengambilan dijelaskan di dialog yang sama supaya
    // tidak ada kejutan saat barang diambil.
    var showClaimForm by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.clearMessage()
    }

    val loadedReport = (uiState as? UiState.Success)?.data
    if (showClaimForm && loadedReport != null) {
        ClaimRequestDialog(
            report = loadedReport,
            onDismiss = { showClaimForm = false },
            onSubmit = { answer ->
                viewModel.submitClaim(answer)
                showClaimForm = false
            }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                myClaim = myClaim,
                canClaim = canClaim,
                canChat = canChat,
                onClaimClick = { showClaimForm = true },
                onOpenReport = onOpenReport,
                onChatClick = onChatClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Formulir pengajuan klaim.
 *
 * Petugas pos tidak bisa menilai kepemilikan tanpa bukti, jadi pengguna wajib
 * menuliskan ciri yang hanya diketahui pemilik (nama di bagian bawah, isi tas,
 * goresan, dsb.). Prosedur pengambilan tetap dijelaskan di dialog ini agar
 * pengguna tahu bahwa barang baru diserahkan setelah KTM dicocokkan.
 */
@Composable
private fun ClaimRequestDialog(
    report: Report,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var answer by remember(report.id) { mutableStateOf("") }
    val isValid = answer.trim().length >= ClaimUiText.MIN_ANSWER_LENGTH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.claim_form_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.claim_info_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.claim_info_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.claim_form_answer_label)) },
                    placeholder = { Text(stringResource(R.string.claim_form_answer_placeholder)) },
                    supportingText = { Text(stringResource(R.string.claim_form_answer_hint)) },
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(answer) },
                enabled = isValid
            ) {
                Text(text = stringResource(R.string.claim_form_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.claim_form_cancel))
            }
        }
    )
}

/**
 * Kartu status klaim milik pengguna aktif pada barang ini.
 *
 * Setelah klaim dikirim, tombol "Ini Barang Saya" diganti kartu ini supaya
 * pengguna tahu klaimnya sedang diperiksa; saat disetujui, kode pengambilan
 * ditampilkan di sini (sumber yang sama dengan notifikasi).
 */
@Composable
private fun MyClaimCard(
    claim: Claim,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (claim.status) {
        ClaimStatus.MENUNGGU -> stringResource(R.string.claim_status_pending) to
            MaterialTheme.colorScheme.tertiary
        ClaimStatus.DISETUJUI -> stringResource(R.string.claim_status_approved) to
            MaterialTheme.colorScheme.primary
        ClaimStatus.DITOLAK -> stringResource(R.string.claim_status_rejected) to
            MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.claim_status_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            when {
                claim.status == ClaimStatus.DISETUJUI && claim.pickupCode != null -> {
                    Text(
                        text = stringResource(
                            R.string.claim_status_pickup_code,
                            claim.pickupCode
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.claim_status_pickup_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                claim.status == ClaimStatus.DITOLAK -> {
                    Text(
                        text = claim.decisionNote?.takeIf { it.isNotBlank() }
                            ?.let { note ->
                                stringResource(R.string.claim_status_rejected_note, note)
                            }
                            ?: stringResource(R.string.claim_status_rejected_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.claim_status_pending_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(R.string.claim_status_your_answer, claim.answer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DetailContent(
    report: Report,
    candidates: List<Report>,
    myClaim: Claim?,
    canClaim: Boolean,
    canChat: Boolean,
    onClaimClick: () -> Unit,
    onChatClick: (String) -> Unit,
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

            // Pintu masuk percakapan (db/0019): muncul untuk laporan orang lain,
            // baik laporan HILANG (chat dengan penemu) maupun TEMUAN (chat dengan
            // pemilik). Petugas juga melihat tombol ini dan ikut memantau utas.
            if (canChat) {
                // Label mengikuti jenis laporan: pada laporan HILANG lawan
                // bicaranya adalah orang yang kehilangan, pada laporan TEMUAN
                // lawan bicaranya adalah penemu.
                val chatLabel = if (report.type == ItemType.FOUND) {
                    R.string.action_chat_found
                } else {
                    R.string.action_chat_lost
                }
                OutlinedButton(
                    onClick = { onChatClick(report.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(chatLabel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Satu pengguna satu klaim per barang: setelah klaim dikirim (atau
            // sudah diputuskan), tombol diganti kartu status supaya tidak ada
            // pengajuan ganda dan kode pengambilan tetap terlihat.
            if (myClaim != null) {
                MyClaimCard(claim = myClaim)
            } else if (canClaim) {
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

            if (candidates.isNotEmpty()) {
                MatchSection(
                    report = report,
                    candidates = candidates,
                    onOpenReport = onOpenReport
                )
            }
        }
    }
}

/**
 * Bagian "Mungkin barangmu": pasangan yang ditemukan server (db/0018) untuk
 * laporan ini. Kartu kandidat membuka detail laporan seberang; klaim diajukan
 * dari halaman detail barang temuan tersebut ("Ini Barang Saya"), sehingga
 * bukti ciri barang dan lokasi pos yang dipakai petugas selalu satu sumber.
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
