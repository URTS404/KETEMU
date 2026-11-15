package org.umn.application_ketemu.ui.feature.admin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.Claim

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
    val allowed by viewModel.isAllowed.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selected by remember { mutableStateOf<Claim?>(null) }

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
        }
    }

    selected?.let { claim ->
        ClaimReviewDialog(
            claim = claim,
            onApprove = {
                viewModel.approve(claim.id)
                selected = null
            },
            onReject = {
                viewModel.reject(claim.id, note = "Bukti belum cukup")
                selected = null
            },
            onDismiss = { selected = null }
        )
    }
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
 * Jawaban verifikasi ditampilkan utuh karena itulah bukti yang dinilai; tanpa
 * membacanya moderator tidak punya dasar untuk menyetujui pengambilan.
 */
@Composable
private fun ClaimReviewDialog(
    claim: Claim,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = claim.reportTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${stringResource(R.string.admin_claim_claimant)}: ${claim.claimantName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.admin_claim_answer_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = claim.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                TextButton(onClick = onReject) {
                    Text(text = stringResource(R.string.admin_action_reject))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.admin_close))
                }
            }
        }
    )
}
