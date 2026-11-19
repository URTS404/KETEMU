package org.umn.application_ketemu.ui.feature.message

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import org.umn.application_ketemu.core.image.ImageCompressor
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.MessageStatus

/**
 * Layar Percakapan (chat 1-on-1).
 *
 * Susunannya mengikuti kebiasaan aplikasi pesan:
 *  - header: nama lawan bicara + perannya (dan konteks barang di atas daftar),
 *  - gelembung (bubble) pesan: dari pengguna di kanan (warna aksen), dari
 *    lawan bicara di kiri (permukaan lembut),
 *  - indikator "sedang menulis..." saat lawan bicara membalas (simulasi),
 *  - kolom tulis + tombol kirim di bagian bawah.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val thread by viewModel.thread.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val canSend by viewModel.canSend.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    // Pesan yang sedang dipilih untuk dihapus (tekan lama pada gelembung).
    var hapusId by remember { mutableStateOf<String?>(null) }
    val konteks = LocalContext.current

    // Pemilih foto bawaan Android (tanpa izin Storage/Media).
    val pemilihFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Kompresi dijalankan di luar thread UI agar layar tidak tersendat.
            Thread {
                val hasil = ImageCompressor.compress(konteks, uri)
                if (hasil != null) {
                    viewModel.onImagePicked(hasil.bytes, hasil.width, hasil.height)
                }
            }.start()
        }
    }
    val onPickImage: () -> Unit = {
        pemilihFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Banner konteks barang = item ke-0, jadi total = 1 + pesan.
    val itemCount = 1 + messages.size
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Scaffold(
        // `imePadding()` membuat seluruh layar (termasuk kolom tulis di
        // bottomBar) naik mengikuti tinggi keyboard. Tanpa ini, dengan
        // edge-to-edge Android 15+ tombol kirim tertutup keyboard.
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = thread?.partnerName
                                ?: stringResource(R.string.nav_message),
                            style = MaterialTheme.typography.titleMedium
                        )
                        thread?.let { info ->
                            Text(
                                text = info.partnerRole,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Muat ulang"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                if (viewModel.templates.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.templates.size) { index ->
                            val template = viewModel.templates[index]
                            AssistChip(
                                onClick = { viewModel.onDraftChange(template.body) },
                                label = { Text(template.label) }
                            )
                        }
                    }
                }
                MessageComposer(
                    value = draft,
                    onValueChange = viewModel::onDraftChange,
                    onSend = { viewModel.onSend() },
                    onPickImage = onPickImage,
                    canSend = canSend
                )
            }
        }
    ) { innerPadding ->
        if (hapusId != null) {
            HapusPesanDialog(
                onHapus = {
                    viewModel.onDelete(hapusId!!)
                    hapusId = null
                },
                onBatal = { hapusId = null }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { ReportContextBanner(title = thread?.reportTitle.orEmpty()) }

            itemsIndexed(items = messages, key = { _, message -> message.id }) { _, message ->
                MessageBubble(
                    message = message,
                    onRetry = viewModel::onRetry,
                    onLongPress = { hapusId = message.id },
                    onDelete = { viewModel.onDelete(message.id) }
                )
            }

        }
    }
}

/**
 * Gelembung satu pesan.
 *
 * Sudut sengaja dibuat asimetris (satu sudut lebih kecil) sebagai "ekor"
 * gelembung. Karena bentuknya memang khusus untuk chat, di sini memakai
 * RoundedCornerShape langsung - bukan MaterialTheme.shapes.
 */
/**
 * Menu titik-tiga pada gelembung milik sendiri.
 *
 * Dipakai sebagai jalur yang lebih pasti daripada tekan lama: ikonnya selalu
 * terlihat, sehingga pengguna tidak perlu menebak bahwa gelembung bisa ditekan.
 */
@Composable
private fun MenuPesan(onHapus: () -> Unit) {
    var terbuka by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { terbuka = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.chat_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_delete_action)) },
                onClick = {
                    terbuka = false
                    onHapus()
                }
            )
        }
    }
}

@Composable
private fun HapusPesanDialog(
    onHapus: () -> Unit,
    onBatal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text(stringResource(R.string.chat_delete_title)) },
        text = { Text(stringResource(R.string.chat_delete_body)) },
        confirmButton = {
            TextButton(onClick = onHapus) {
                Text(stringResource(R.string.chat_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onBatal) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onRetry: (String) -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val isMine = message.fromMe

    // Pesan yang belum terkirim diberi tombol putar ulang di samping gelembung,
    // sama seperti WhatsApp: pengguna bisa mencoba lagi tanpa mengetik ulang.
    val bisaDiulang = isMine && message.status != MessageStatus.SENT

    val bubbleShape = if (isMine) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 4.dp,
            bottomStart = 18.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            bottomStart = 4.dp
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (bisaDiulang) {
            IconButton(onClick = { onRetry(message.id) }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.chat_retry),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Surface(
                modifier = if (isMine) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress
                    )
                } else {
                    Modifier
                },
                shape = bubbleShape,
                color = if (isMine) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isMine) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ) {
                Column {
                    if (message.hasImage) {
                        // Gambar yang belum terkirim sengaja dibuat BUARU + ikon di
                        // tengah, mengikuti pola WhatsApp: isi gambar tetap terlihat
                        // tetapi jelas belum sampai ke lawan bicara.
                        val belumTerkirim = message.status != MessageStatus.SENT
                        Box(
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .heightIn(max = 260.dp)
                                .clip(bubbleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = message.imageUrl ?: message.localImagePath,
                                contentDescription = stringResource(R.string.chat_image),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (belumTerkirim) Modifier.blur(10.dp) else Modifier
                                    ),
                                contentScale = ContentScale.Crop
                            )
                            if (belumTerkirim) {
                                IconButton(
                                    onClick = { onRetry(message.id) },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = stringResource(R.string.chat_retry),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Pesan yang belum sampai server tetap terlihat (seperti WhatsApp):
            // waktu + penanda antrean, supaya pengguna tahu pesannya tidak hilang.
            val waktu = RelativeTime.format(message.sentAt)
            val keterangan = when (message.status) {
                MessageStatus.SENDING -> "$waktu - menunggu terkirim"
                MessageStatus.FAILED -> "$waktu - gagal terkirim"
                MessageStatus.SENT -> waktu
            }
            if (isMine) {
                MenuPesan(onHapus = onDelete)
            }

            Text(
                text = keterangan,
                modifier = Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (message.status == MessageStatus.SENT) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

/** Indikator "sedang menulis..." dengan denyut halus. */
@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typingPulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.alpha(pulse),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = stringResource(R.string.chat_typing),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Banner kecil di atas percakapan: barang yang sedang dibicarakan. */
@Composable
private fun ReportContextBanner(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column {
                Text(
                    text = stringResource(R.string.chat_about_report),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2
                )
            }
        }
    }
}

/** Kolom tulis pesan + tombol kirim. */
@Composable
private fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    canSend: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onPickImage, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.chat_attach),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                maxLines = 4
            )

            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                containerColor = if (canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (canSend) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
