package org.umn.application_ketemu.ui.feature.message

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.umn.application_ketemu.R
import org.umn.application_ketemu.ui.components.EmptyState

/**
 * Layar Pesan.
 *
 * Tahap ini masih kerangka (placeholder): fitur percakapan antara pemilik dan
 * penemu barang akan diimplementasikan pada fase berikutnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_message)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.message_empty_title),
            description = stringResource(R.string.message_empty_desc),
            modifier = Modifier.padding(innerPadding)
        )
    }
}
