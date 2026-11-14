package org.umn.application_ketemu.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.ui.components.EmptyState

/** Log aktivitas moderator: riwayat keputusan, terbaru di atas, tidak bisa diubah. */
@Composable
fun AdminAuditScreen(
    viewModel: AdminAuditViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val allowed by viewModel.isAllowed.collectAsStateWithLifecycle()

    AdminScaffold(
        title = stringResource(R.string.admin_menu_audit),
        onBack = onBack
    ) { padding ->
        if (!allowed) {
            AdminAccessDenied(modifier = Modifier.padding(padding))
            return@AdminScaffold
        }

        if (entries.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.admin_audit_empty),
                description = stringResource(R.string.admin_audit_empty_desc),
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = entries, key = { it.id }) { entry ->
                AuditRow(entry = entry)
                HorizontalDivider()
            }
        }
    }
}
