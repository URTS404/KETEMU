package org.umn.application_ketemu.ui.feature.detail

import org.umn.application_ketemu.ui.components.PesanSnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import coil3.compose.AsyncImagePainter
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
    val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
    val sedangHapus by viewModel.sedangHapus.collectAsStateWithLifecycle()
    val terhapus by viewModel.terhapus.collectAsStateWithLifecycle()
    val sedangBatalKlaim by viewModel.sedangBatalKlaim.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Formulir klaim: pemilik menuliskan ciri barang, lalu petugas pos yang
    // memverifikasi. Prosedur pengambilan dijelaskan di dialog yang sama supaya
    // tidak ada kejutan saat barang diambil.
    var showClaimForm by remember { mutableStateOf(false) }

    // Menu "Hapus laporan": hanya untuk pemilik, dan selalu lewat konfirmasi
    // karena tindakannya permanen (berbeda dengan Arsipkan milik petugas).
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOwnerMenu by remember { mutableStateOf(false) }

    // Setelah laporan benar-benar terhapus, halaman detail tidak punya data lagi,
    // jadi langsung kembali ke layar sebelumnya.
    LaunchedEffect(terhapus) {
        if (terhapus) onBack()
    }

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

    if (showDeleteDialog) {
        HapusLaporanDialog(
            sedangHapus = sedangHapus,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.hapusLaporan()
            }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { PesanSnackbarHost(hostState = snackbarHostState) },
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
                ),
                actions = {
                    // Pemilik boleh menghapus laporannya sendiri (salah kirim,
                    // atau barangnya sudah ditemukan sendiri). Petugas memakai
                    // Arsipkan di panel admin, bukan tombol ini.
                    if (isOwner && loadedReport != null) {
                        Box {
                            IconButton(
                                onClick = { showOwnerMenu = true },
                                enabled = !sedangHapus
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.detail_menu_open)
                                )
                            }
                            DropdownMenu(
                                expanded = showOwnerMenu,
                                onDismissRequest = { showOwnerMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_delete_report)) },
                                    onClick = {
                                        showOwnerMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
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
                sedangBatalKlaim = sedangBatalKlaim,
                onBatalkanKlaim = { viewModel.batalkanKlaim() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Konfirmasi hapus laporan milik sendiri.
 *
 * Penghapusan bersifat permanen (baris di server ikut hilang beserta klaim dan
 * pencocokan yang menunjuknya), jadi selalu lewat dialog Ã¢â‚¬â€ bukan sekali tekan.
 * Kasus pemakaiannya: salah kirim laporan, atau barang hilang sudah ditemukan
 * sendiri sehingga laporannya tidak perlu dicari lagi.
 */
@Composable
private fun HapusLaporanDialog(
    sedangHapus: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_delete_title)) },
        text = { Text(stringResource(R.string.detail_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !sedangHapus) {
                Text(stringResource(R.string.detail_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_delete_cancel))
            }
        }
    )
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
    modifier: Modifier = Modifier,
    sedangBatal: Boolean = false,
    onBatalkan: () -> Unit = {}
) {
    val sudahDiambil = claim.handedOverAt != null
    val (statusText, statusColor) = when {
        // Serah terima sudah dicatat petugas: kode pengambilan tidak relevan lagi.
        sudahDiambil -> stringResource(R.string.claim_status_taken) to
            MaterialTheme.colorScheme.primary
        claim.status == ClaimStatus.MENUNGGU -> stringResource(R.string.claim_status_pending) to
            MaterialTheme.colorScheme.tertiary
        claim.status == ClaimStatus.DISETUJUI -> stringResource(R.string.claim_status_approved) to
            MaterialTheme.colorScheme.primary
        else -> stringResource(R.string.claim_status_rejected) to
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
                sudahDiambil -> {
                    Text(
                        text = stringResource(R.string.claim_status_taken_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
    sedangBatalKlaim: Boolean,
    onBatalkanKlaim: () -> Unit,
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
                    text = report.pos?.let { "${it.name} Ã‚Â· ${it.building}" }
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
                text = "Dilaporkan oleh ${report.ownerName} Ã‚Â· " +
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
                MyClaimCard(
                    claim = myClaim,
                    sedangBatal = sedangBatalKlaim,
                    onBatalkan = onBatalkanKlaim
                )
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
    // Foto bisa gagal dimuat (URI lokal perangkat pembuat laporan yang sudah tidak
    // bisa diakses). Lebih jujur menampilkan "belum ada foto" daripada kotak kosong.
    var gagalMuat by remember(report.photoUri) { mutableStateOf(false) }

    if (report.photoUri.isNullOrBlank() || gagalMuat) {
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
        // Foto bisa diketuk untuk diperbesar, supaya pengguna dapat memeriksa
        // rincian barang (jahitan, logo, warna) sebelum mengajukan klaim.
        var tampilZoom by remember { mutableStateOf(false) }

        AsyncImage(
            model = report.photoUri,
            contentDescription = report.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .clickable { tampilZoom = true },
            onState = { keadaan ->
                if (keadaan is AsyncImagePainter.State.Error) gagalMuat = true
            }
        )

        if (tampilZoom) {
            FotoZoomDialog(
                uri = report.photoUri,
                judul = report.title,
                onDismiss = { tampilZoom = false }
            )
        }
    }
}

/**
 * Menampilkan foto laporan ukuran penuh dengan cubit/geser untuk memperbesar.
 * Latar hitam dipakai agar detail foto lebih mudah diperiksa.
 */
@Composable
private fun FotoZoomDialog(uri: String, judul: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var skala by remember { mutableStateOf(1f) }
        var geser by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = judul,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            skala = (skala * zoom).coerceIn(1f, 5f)
                            geser = if (skala > 1f) geser + pan else Offset.Zero
                        }
                    }
                    .graphicsLayer {
                        scaleX = skala
                        scaleY = skala
                        translationX = geser.x
                        translationY = geser.y
                    }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.report_photo_close),
                    tint = Color.White
                )
            }

            Text(
                text = stringResource(R.string.report_photo_zoom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            )
        }
    }
}
