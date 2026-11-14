package org.umn.application_ketemu.ui.feature.message

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.ConversationThread
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.components.InitialsAvatar
import org.umn.application_ketemu.ui.components.LoadingSkeletonList

/** Durasi animasi isi & kartu halaman Pesan (milidetik). */
private const val CONTENT_MS = 260
private const val ITEM_MS = 280

/** Jenis keadaan layar - dipakai sebagai KUNCI animasi. */
private enum class MessageStateKind { LOADING, EMPTY, ERROR, SUCCESS }

/**
 * Layar Pesan: daftar percakapan pengguna dengan penemu / pemilik barang.
 *
 * Setiap percakapan terhubung ke satu laporan (lihat [ConversationThread]),
 * sehingga pengguna selalu tahu percakapan itu tentang barang yang mana.
 * Menekan satu baris membuka layar Percakapan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    viewModel: MessagesViewModel,
    onThreadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        val stateKind = when (uiState) {
            UiState.Loading -> MessageStateKind.LOADING
            UiState.Empty -> MessageStateKind.EMPTY
            is UiState.Error -> MessageStateKind.ERROR
            is UiState.Success -> MessageStateKind.SUCCESS
        }

        AnimatedContent(
            targetState = stateKind,
            transitionSpec = {
                (fadeIn(tween(CONTENT_MS)) +
                    slideInVertically(tween(CONTENT_MS)) { full -> full / 12 }) togetherWith
                    fadeOut(tween(CONTENT_MS / 2))
            },
            label = "messageContent",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { kind ->
            when (kind) {
                MessageStateKind.LOADING -> LoadingSkeletonList(itemCount = 3)

                MessageStateKind.EMPTY -> EmptyState(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.message_empty_title),
                    description = stringResource(R.string.message_empty_desc)
                )

                MessageStateKind.ERROR -> EmptyState(
                    title = stringResource(R.string.error_generic_title),
                    description = stringResource(R.string.error_generic_desc)
                )

                MessageStateKind.SUCCESS -> ConversationList(
                    threads = (uiState as? UiState.Success)?.data.orEmpty(),
                    onThreadClick = onThreadClick
                )
            }
        }
    }
}

/** Daftar percakapan dengan animasi masuk berurutan (stagger). */
@Composable
private fun ConversationList(
    threads: List<ConversationThread>,
    onThreadClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items = threads, key = { _, thread -> thread.id }) { index, thread ->
            var visible by remember { mutableStateOf(false) }
            val delayMillis = (index % 6) * 55
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(ITEM_MS, delayMillis = delayMillis)) +
                    slideInVertically(tween(ITEM_MS, delayMillis = delayMillis)) { it / 4 }
            ) {
                ConversationRow(
                    thread = thread,
                    onClick = { onThreadClick(thread.id) }
                )
            }
        }
    }
}

/**
 * Satu baris percakapan: avatar, nama lawan bicara, perannya, cuplikan pesan
 * terakhir, konteks barang, dan badge jumlah pesan belum dibaca.
 */
@Composable
private fun ConversationRow(
    thread: ConversationThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InitialsAvatar(name = thread.partnerName, size = 48.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.partnerName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = RelativeTime.format(thread.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = thread.partnerRole,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = thread.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.chat_about_report),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = thread.reportTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (thread.unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary
                ) {
                    Text(
                        text = thread.unreadCount.toString(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
        }
    }
}
