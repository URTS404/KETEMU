package org.umn.application_ketemu.ui.feature.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.components.NotificationListItem

/**
 * Kotak masuk notifikasi.
 *
 * Menampilkan seluruh kabar yang relevan bagi pengguna aktif: konfirmasi
 * laporan sendiri, siaran barang temuan baru, perubahan status oleh petugas,
 * dan keputusan klaim. Menekan satu baris menandainya terbaca dan — bila
 * notifikasi menunjuk laporan — langsung membuka detail laporan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::markAllRead,
                        enabled = state.unreadCount > 0
                    ) {
                        Text(stringResource(R.string.notifications_mark_all))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.notifications_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            state.isEmpty -> EmptyState(
                title = stringResource(R.string.notifications_empty_title),
                description = stringResource(R.string.notifications_empty_desc),
                icon = Icons.Default.Notifications,
                modifier = Modifier.padding(innerPadding)
            )

            else -> NotificationList(
                state = state,
                contentPadding = innerPadding,
                onMarkRead = viewModel::markRead,
                onOpenReport = onOpenReport
            )
        }
    }
}

/** Daftar notifikasi beserta ringkasan di atasnya. */
@Composable
private fun NotificationList(
    state: NotificationsUiState,
    contentPadding: PaddingValues,
    onMarkRead: (String) -> Unit,
    onOpenReport: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") {
            NotificationSummary(
                unreadCount = state.unreadCount,
                total = state.notifications.size
            )
        }

        items(items = state.notifications, key = { it.id }) { notification ->
            NotificationListItem(
                notification = notification,
                onClick = {
                    onMarkRead(notification.id)
                    notification.reportId?.let(onOpenReport)
                }
            )
        }

        item(key = "hint") {
            Text(
                text = stringResource(R.string.notifications_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** Ringkasan di atas daftar: berapa notifikasi yang belum dibaca. */
@Composable
private fun NotificationSummary(unreadCount: Int, total: Int) {
    val hasUnread = unreadCount > 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (hasUnread) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (hasUnread) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = if (hasUnread) {
                    stringResource(R.string.notifications_summary, unreadCount, total)
                } else {
                    stringResource(R.string.notifications_all_read)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasUnread) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
