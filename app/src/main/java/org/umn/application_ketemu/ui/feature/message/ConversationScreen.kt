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
import androidx.compose.foundation.clickable
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
import org.umn.application_ketemu.ui.components.PesanSnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
    // Saat layar kembali dibuka, muat ulang dari server: signed URL gambar
    // hanya berlaku 1 jam, jadi gambar lama perlu URL baru agar tetap tampil.
    val pemilikSiklus = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(pemilikSiklus) {
        val pengamat = androidx.lifecycle.LifecycleEventObserver { _, peristiwa ->
            if (peristiwa == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.onRefresh()
        }
        pemilikSiklus.lifecycle.addObserver(pengamat)
        onDispose { pemilikSiklus.lifecycle.removeObserver(pengamat) }
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val peerTyping by viewModel.peerTyping.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.pantauMengetik() }

    // Model gambar yang sedang diperbesar (null = tidak ada pratinjau).
    var gambarPratinjau by remember { mutableStateOf<String?>(null) }

    gambarPratinjau?.let { model ->
        ImageZoomOverlay(model = model, onClose = { gambarPratinjau = null })
    }
    val thread by viewModel.thread.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val canSend by viewModel.canSend.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val pesanGalat by viewModel.pesan.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pesanGalat) {
        val teks = pesanGalat
        if (teks != null) {
            snackbarHostState.showSnackbar(teks)
            viewModel.pesanDibaca()
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Pesan yang sedang dipilih untuk dihapus (tekan lama pada gelembung).
    var hapusId by remember { mutableStateOf<String?>(null) }
    val konteks = LocalContext.current

    // Pemilih foto bawaan Android (tanpa izin Storage/Media).
    val pemilihFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        android.util.Log.d("Chat", "pemilih foto kembali: uri=" + uri)
        if (uri == null) return@rememberLauncherForActivityResult
        // Kompresi dijalankan di luar thread UI agar layar tidak tersendat.
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val hasil = runCatching { ImageCompressor.compress(konteks, uri) }
                .onFailure { android.util.Log.e("Chat", "kompresi gambar gagal: " + it) }
                .getOrNull()
            if (hasil == null) {
                android.util.Log.e("Chat", "kompresi gambar mengembalikan null untuk " + uri)
                return@launch
            }
            android.util.Log.d("Chat", "kompresi selesai: " + hasil.bytes.size + " byte, " + hasil.width + "x" + hasil.height)
            viewModel.onImagePicked(hasil.bytes, hasil.width, hasil.height)
        }
    }
    val onPickImage: () -> Unit = {
        pemilihFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Gulir otomatis ke pesan terbaru: saat layar dibuka, saat pesan masuk
    // (termasuk dari lawan bicara lewat Realtime), dan saat indikator "sedang
    // mengetik" muncul. Daftar memakai reverseLayout, jadi index 0 adalah
    // posisi paling bawah - menggulir ke 0 berarti menempel ke pesan terakhir.
    //
    // Syarat "hanya bila pengguna sudah di dasar": tanpa syarat ini, orang yang
    // sedang membaca pesan lama akan terlempar ke bawah setiap ada pesan baru
    // (regresi yang pernah diperbaiki di commit e5d78c0). Indeks 0/1 dipakai
    // karena item ke-0 bisa berupa indikator "sedang mengetik".
    LaunchedEffect(messages.size, peerTyping) {
        val info = listState.layoutInfo
        if (info.totalItemsCount > 0) {
            val teratas = info.visibleItemsInfo.firstOrNull()?.index ?: 0
            if (teratas <= 1) listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        // `imePadding()` membuat seluruh layar (termasuk kolom tulis di
        // bottomBar) naik mengikuti tinggi keyboard. Tanpa ini, dengan
        // edge-to-edge Android 15+ tombol kirim tertutup keyboard.
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { PesanSnackbarHost(snackbarHostState) },
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
                    onSend = {
                        viewModel.onSend()
                        scope.launch {
                            kotlinx.coroutines.delay(350)
                            val total = listState.layoutInfo.totalItemsCount
                            if (total > 0) listState.animateScrollToItem(0)
                        }
                    },
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
            // reverseLayout: index 0 berada di DASAR layar. Dengan pola ini,
            // daftar selalu menempel ke bawah - tidak ada lagi lompatan ke atas
            // ketika gambar selesai dimuat atau pesan baru masuk.
            reverseLayout = true
        ) {
            // reverseLayout: index 0 = paling BAWAH. Indikator "sedang mengetik"
            // harus berada di index 0 agar tampil tepat di bawah pesan terbaru.
            // Sebelumnya ia diletakkan setelah daftar pesan sehingga tergambar
            // di ATAS riwayat percakapan - di luar layar untuk utas panjang,
            // sehingga indikatornya tampak "tidak muncul" sama sekali.
            if (peerTyping) {
                item(key = "sedang-mengetik") { TypingBubble() }
            }

            itemsIndexed(
                items = messages.asReversed(),
                key = { _, message -> message.id }
            ) { _, message ->
                MessageBubble(
                    message = message,
                    onRetry = viewModel::onRetry,
                    onImageClick = { gambarPratinjau = it },
                    onLongPress = { hapusId = message.id },
                    onDelete = { viewModel.onDelete(message.id) }
                )
            }

            // Banner konteks barang sengaja di akhir: pada reverseLayout ia
            // tampil paling atas, seperti judul utas.
            item(key = "konteks-laporan") {
                ReportContextBanner(title = thread?.reportTitle.orEmpty())
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
                    android.util.Log.d("Chat", "menu hapus diketuk")
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
    onImageClick: (String) -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val isMine = message.fromMe

    // Waktu + status dihitung di awal supaya label bisa diletakkan DI DALAM
    // gelembung (lebar kolom = lebar gelembung, sehingga ikon putar ulang
    // menempel rapat di samping kiri gelembung, bukan tertinggal jauh).
    // Jam kirim (HH:mm) lebih informatif daripada "1 jam lalu".
    val waktu = try {
        java.time.Instant.ofEpochMilli(message.sentAt)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        ""
    }
    val keterangan = when (message.status) {
        MessageStatus.SENDING -> "" + waktu + " - menunggu terkirim"
        MessageStatus.FAILED -> "" + waktu + " - gagal terkirim"
        MessageStatus.SENT -> waktu
    }

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
        // Untuk pesan bergambar, ikon putar ulang sudah tampil di tengah
        // gambar — jangan digambar dua kali.
        if (bisaDiulang && !message.hasImage) {
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
                                    )
                                    .clickable {
                                        val model = message.imageUrl ?: message.localImagePath
                                        if (model != null) onImageClick(model)
                                    },
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

                    Text(
                        text = keterangan,
                        modifier = Modifier.padding(
                            start = 14.dp, end = 14.dp, top = 0.dp, bottom = 3.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            message.status != MessageStatus.SENT && isMine ->
                                MaterialTheme.colorScheme.onPrimary
                            !isMine -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        }
                    )
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
