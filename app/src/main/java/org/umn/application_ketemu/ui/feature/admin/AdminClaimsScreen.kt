package org.umn.application_ketemu.ui.feature.admin

import org.umn.application_ketemu.ui.components.PesanSnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.Report

/**
 * Antrean klaim untuk moderator.
 *
 * Jawaban verifikasi ditampilkan apa adanya (itu bukti yang dinilai petugas),
 * lalu moderator memutuskan setuju/tolak. Menyetujui klaim menerbitkan kode
 * pengambilan yang muncul pada snackbar dan pada layar detail laporan.
 *
 * Tampilan mengikuti referensi desain ruang admin: header besar, kartu ringkasan
 * amber bergradasi, judul bagian "Klaim masuk", lalu kartu klaim putih berisi
 * status, waktu masuk, identitas pengklaim, dan tombol "Tinjau klaim" selebar
 * kartu.
 */
@Composable
fun AdminClaimsScreen(
    viewModel: AdminClaimsViewModel,
    onBack: () -> Unit
) {
    val claims by viewModel.pendingClaims.collectAsStateWithLifecycle()
    val siapDiambil by viewModel.readyForPickup.collectAsStateWithLifecycle()
    val reportsById by viewModel.reportsById.collectAsStateWithLifecycle()
    val allowed by viewModel.isAllowed.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selected by remember { mutableStateOf<Claim?>(null) }
    var handoverTarget by remember { mutableStateOf<Claim?>(null) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.clearFeedback()
    }

    // Pengguna tanpa hak tetap melihat kerangka panel + pesan penolakan.
    if (!allowed) {
        AdminScaffold(
            title = stringResource(R.string.admin_menu_claims),
            onBack = onBack
        ) { padding ->
            AdminAccessDenied(modifier = Modifier.padding(padding))
        }
        return
    }

    val tones = rememberAdminTones()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { PesanSnackbarHost(hostState = snackbarHostState) }
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
                        title = stringResource(R.string.admin_menu_claims),
                        subtitle = stringResource(R.string.admin_claims_subtitle),
                        onBack = onBack
                    )
                }
            }

            item(key = "summary") {
                AdminReveal(index = 1) {
                    AdminSummaryCard(
                        label = stringResource(R.string.admin_claims_pending_label),
                        count = claims.size,
                        unit = stringResource(R.string.admin_claims_pending_unit),
                        iconRes = R.drawable.ic_admin_clipboard_check,
                        tone = tones.amber,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item(key = "section") {
                AdminReveal(index = 2) {
                    AdminSectionHeader(
                        title = stringResource(R.string.admin_claims_queue_section),
                        modifier = Modifier.padding(top = 8.dp),
                        trailing = {
                            Text(
                                // Jumlah pada judul bagian memakai sumber data yang
                                // sama dengan kartu ringkasan, jadi selalu cocok.
                                text = stringResource(R.string.admin_claims_queue_count, claims.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (claims.isEmpty()) {
                item(key = "empty") {
                    AdminReveal(index = 3) { ClaimsEmptyCard() }
                }
            } else {
                itemsIndexed(items = claims, key = { _, claim -> claim.id }) { index, claim ->
                    AdminReveal(index = index + 3, modifier = Modifier.animateItem()) {
                        ClaimCard(
                            claim = claim,
                            posName = reportsById[claim.reportId]?.pos?.name,
                            onReview = { selected = claim }
                        )
                    }
                }
            }

            item(key = "hint") {
                AdminReveal(index = claims.size + 3) {
                    AdminHintRow(
                        text = stringResource(R.string.admin_claims_hint),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Bagian kedua: klaim yang sudah disetujui, menunggu barangnya
            // benar-benar diserahkan. Inilah langkah penutup alur klaim.
            item(key = "pickup-section") {
                AdminReveal(index = claims.size + 4) {
                    AdminSectionHeader(
                        title = stringResource(R.string.admin_pickup_section),
                        modifier = Modifier.padding(top = 16.dp),
                        trailing = {
                            Text(
                                text = stringResource(R.string.admin_claims_queue_count, siapDiambil.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (siapDiambil.isEmpty()) {
                item(key = "pickup-empty") {
                    AdminReveal(index = claims.size + 5) { PickupEmptyCard() }
                }
            } else {
                itemsIndexed(items = siapDiambil, key = { _, claim -> "pickup-${claim.id}" }) { index, claim ->
                    AdminReveal(index = claims.size + 5 + index, modifier = Modifier.animateItem()) {
                        PickupCard(
                            claim = claim,
                            posName = reportsById[claim.reportId]?.pos?.name,
                            onHandover = { handoverTarget = claim }
                        )
                    }
                }
            }

            item(key = "pickup-hint") {
                AdminReveal(index = claims.size + siapDiambil.size + 5) {
                    AdminHintRow(
                        text = stringResource(R.string.admin_pickup_hint),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    selected?.let { claim ->
        ClaimReviewDialog(
            claim = claim,
            report = reportsById[claim.reportId],
            onApprove = {
                viewModel.approve(claim.id)
                selected = null
            },
            onReject = { note ->
                viewModel.reject(claim.id, note = note)
                selected = null
            },
            onDismiss = { selected = null }
        )
    }

    handoverTarget?.let { claim ->
        HandoverDialog(
            claim = claim,
            posName = reportsById[claim.reportId]?.pos?.name,
            onConfirm = { code ->
                viewModel.completeHandover(claim, code)
                handoverTarget = null
            },
            onDismiss = { handoverTarget = null }
        )
    }
}

/**
 * Keadaan kosong untuk bagian "Menunggu pengambilan".
 *
 * Dipisahkan dari [ClaimsEmptyCard] karena artinya berbeda: di sini tidak ada
 * pekerjaan yang tertunda, bukan berarti belum ada klaim masuk.
 */
@Composable
private fun PickupEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_pickup_empty),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.admin_pickup_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Satu klaim yang barangnya sudah siap diambil.
 *
 * Petugas memakai kartu ini di meja pos: lihat siapa pemiliknya, di pos mana
 * barangnya, lalu tekan "Serah terima" untuk mencatat pengambilan.
 */
@Composable
private fun PickupCard(
    claim: Claim,
    posName: String?,
    onHandover: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tones = rememberAdminTones()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = tones.mint.container,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.admin_pickup_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = tones.mint.content,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = RelativeTime.format(claim.decidedAt ?: claim.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = claim.reportTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            ClaimInfoRow(
                label = stringResource(R.string.admin_pickup_owner),
                value = claim.claimantName
            )

            if (!posName.isNullOrBlank()) {
                ClaimInfoRow(
                    label = stringResource(R.string.admin_claim_pos_label),
                    value = posName
                )
            }

            Button(
                onClick = onHandover,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.admin_action_handover))
            }
        }
    }
}

/**
 * Dialog pencatatan serah terima.
 *
 * Kode pengambilan bersifat opsional: petugas boleh mengosongkannya ketika
 * identitas sudah diperiksa lewat KTM. Validasi kode dilakukan database, jadi
 * dialog ini tidak menebak sendiri sah/tidaknya.
 */
@Composable
private fun HandoverDialog(
    claim: Claim,
    posName: String?,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember(claim.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.admin_pickup_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = claim.reportTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.admin_pickup_owner) + ": " + claim.claimantName,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!posName.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.admin_claim_pos_label) + ": " + posName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(6) },
                    label = { Text(stringResource(R.string.admin_pickup_code_label)) },
                    placeholder = { Text(stringResource(R.string.admin_pickup_code_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.admin_pickup_code_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(code.trim().ifEmpty { null }) }) {
                Text(text = stringResource(R.string.admin_pickup_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.admin_close))
            }
        }
    )
}

/**
 * Keadaan kosong di dalam daftar klaim.
 *
 * Dibuat sebagai kartu (bukan layar kosong penuh) supaya header, kartu ringkasan
 * "0 klaim", dan petunjuk di bawahnya tetap tampil seperti layar kerja biasa.
 */
@Composable
private fun ClaimsEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_claims_empty),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.admin_claims_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Satu klaim pada antrean.
 *
 * Urutan bacaan mengikuti pekerjaan moderator: status & waktu masuk -> barang apa
 * -> siapa pengklaimnya -> aksi tinjau. Tombol "Tinjau klaim" dibuat selebar
 * kartu karena itu satu-satunya aksi pada kartu ini.
 */
@Composable
private fun ClaimCard(
    claim: Claim,
    posName: String?,
    onReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tones = rememberAdminTones()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = tones.amber.container
                ) {
                    Text(
                        text = stringResource(R.string.admin_claims_card_status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tones.amber.content
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = RelativeTime.format(claim.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(tones.blue.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_admin_clipboard_check),
                        contentDescription = null,
                        tint = tones.blue.content,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = claim.reportTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${stringResource(R.string.admin_claim_claimant)}: ${claim.claimantName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = claim.claimantEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                posName?.let { name ->
                    Text(
                        text = stringResource(R.string.admin_claims_card_pos, name),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tones.amber.content
                    )
                }
            }

            Button(
                onClick = onReview,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = stringResource(R.string.admin_action_review))
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dialog keputusan klaim.
 *
 * Isinya mengikuti urutan kerja satpam di meja pos: barang apa yang sedang
 * dipegang (judul, deskripsi, pos penitipan), siapa pengklaimnya, lalu jawaban
 * ciri barang yang ia tulis. Kolom alasan penolakan opsional dikirim ke
 * pengklaim lewat notifikasi, sehingga keputusan tidak pernah "bisu".
 */
@Composable
private fun ClaimReviewDialog(
    claim: Claim,
    report: Report?,
    onApprove: () -> Unit,
    onReject: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var rejectNote by remember(claim.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = claim.reportTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClaimInfoRow(
                    label = stringResource(R.string.admin_claim_item_label),
                    value = report?.description ?: claim.reportTitle
                )
                report?.pos?.let { pos ->
                    ClaimInfoRow(
                        label = stringResource(R.string.admin_claim_pos_label),
                        value = "${pos.name} (${pos.building})"
                    )
                }
                report?.ownerName?.let { owner ->
                    ClaimInfoRow(
                        label = stringResource(R.string.admin_claim_owner_label),
                        value = owner
                    )
                }
                ClaimInfoRow(
                    label = stringResource(R.string.admin_claim_claimant),
                    value = "${claim.claimantName} · ${claim.claimantEmail}"
                )
                ClaimInfoRow(
                    label = stringResource(R.string.admin_claim_answer_label),
                    value = claim.answer
                )
                OutlinedTextField(
                    value = rejectNote,
                    onValueChange = { rejectNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.admin_claim_reject_note_label)) },
                    supportingText = {
                        Text(stringResource(R.string.admin_claim_reject_note_hint))
                    },
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = onApprove) {
                Text(text = stringResource(R.string.admin_action_approve))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onReject(rejectNote.trim().ifBlank { null }) }
                ) {
                    Text(text = stringResource(R.string.admin_action_reject))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.admin_close))
                }
            }
        }
    )
}

/**
 * Satu baris label-nilai di dialog tinjauan klaim.
 *
 * Label memakai gaya kecil dan nilai memakai gaya isi supaya mata satpam
 * langsung jatuh ke isi (deskripsi barang, jawaban pengklaim).
 */
@Composable
private fun ClaimInfoRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
