package org.umn.application_ketemu.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.ui.components.EmptyState

/**
 * Antrean klaim untuk moderator.
 *
 * Jawaban verifikasi ditampilkan apa adanya (itu bukti yang dinilai petugas),
 * lalu moderator memutuskan setuju/tolak. Menyetujui klaim menerbitkan kode
 * pengambilan yang muncul pada snackbar dan pada layar detail laporan.
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

    AdminScaffold(
        title = stringResource(R.string.admin_menu_claims),
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        if (!allowed) {
            AdminAccessDenied(modifier = Modifier.padding(padding))
            return@AdminScaffold
        }

        if (claims.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.admin_claims_empty),
                description = stringResource(R.string.admin_claims_empty_desc),
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = claims, key = { it.id }) { claim ->
                ClaimCard(claim = claim, onReview = { selected = claim })
            }
        }
    }

    val claim = selected
    if (claim != null) {
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

@Composable
private fun ClaimCard(claim: Claim, onReview: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = claim.reportTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${stringResource(R.string.admin_claim_claimant)}: ${claim.claimantName} (${claim.claimantEmail})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Waktu masuk dan tombol tinjau berbagi satu baris agar kartu tetap
            // ringkas saat antrean klaim panjang.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = RelativeTime.format(claim.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onReview) {
                    Text(text = stringResource(R.string.admin_action_review))
                }
            }
        }
    }
}

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
