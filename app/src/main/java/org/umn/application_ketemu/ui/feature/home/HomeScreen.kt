package org.umn.application_ketemu.ui.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.umn.application_ketemu.domain.model.HasilCariFoto
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.components.InitialsAvatar
import org.umn.application_ketemu.ui.components.LoadingSkeletonList
import org.umn.application_ketemu.ui.components.ReportPhotoCard
import org.umn.application_ketemu.ui.components.label
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Layar Beranda (Home Feed).
 *
 * Elemen wajib PRD ada di sini: filter Hilang/Ditemukan, pencarian, daftar
 * berbentuk Card, dan tombol Call-to-Action berwarna Orange.
 *
 * Catatan MVVM: state input (kata kunci & filter) disimpan di layar agar bisa
 * dipulihkan saat rotasi (rememberSaveable), lalu diteruskan ke ViewModel yang
 * melakukan penyaringan data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onReportClick: (String) -> Unit,
    onCreateReportClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val unreadNotifications by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf<ItemType?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf<ReportCategory?>(null) }

    val sedangMencariFoto by viewModel.sedangMencariFoto.collectAsStateWithLifecycle()
    val hasilCariFoto by viewModel.hasilCariFoto.collectAsStateWithLifecycle()
    val galatCariFoto by viewModel.galatCariFoto.collectAsStateWithLifecycle()
    var tampilkanHasilFoto by rememberSaveable { mutableStateOf(false) }

    // Pengambil foto dari galeri untuk fitur cari-dengan-foto. Pemilih foto
    // sistem (PickVisualMedia) tidak butuh izin penyimpanan sama sekali.
    val pilihFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            tampilkanHasilFoto = true
            viewModel.cariDenganFoto(uri.toString())
        }
    }

    LaunchedEffect(query) { viewModel.onSearchQueryChange(query) }
    LaunchedEffect(selectedType) { viewModel.onTypeFilterChange(selectedType) }
    LaunchedEffect(selectedCategory) { viewModel.onCategoryFilterChange(selectedCategory) }

    val isFiltering = query.isNotBlank() || selectedType != null || selectedCategory != null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ketemu_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    NotificationButton(
                        count = unreadNotifications,
                        onClick = onOpenNotifications
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.action_report_lost)) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = onCreateReportClick,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { innerPadding ->
        // Pull-to-refresh: tarik ke bawah untuk memuat ulang Beranda.
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bagian atas (sapaan, pengumuman, pencarian) membentang penuh
                // selebar layar; daftar barang di bawahnya disusun 2 kolom.
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HomeGreetingCard(userName = user.fullName, stats = stats)
                }

                item(span = { GridItemSpan(maxLineSpan) }) { AnnouncementCard() }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchAndFilterBar(
                        query = query,
                        onQueryChange = { query = it },
                        selectedType = selectedType,
                        onTypeChange = { selectedType = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        onCariFotoClick = {
                            pilihFoto.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                when (val state = uiState) {
                    UiState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        LoadingSkeletonList()
                    }

                    UiState.Empty -> item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = stringResource(
                                if (isFiltering) R.string.empty_search_title
                                else R.string.empty_reports_title
                            ),
                            description = stringResource(
                                if (isFiltering) R.string.empty_search_desc
                                else R.string.empty_reports_desc
                            ),
                            icon = if (isFiltering) Icons.Default.Search else Icons.Default.Info,
                            actionLabel = if (isFiltering) null else stringResource(
                                R.string.action_report_lost
                            ),
                            onAction = if (isFiltering) null else onCreateReportClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        )
                    }

                    is UiState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = stringResource(R.string.error_generic_title),
                            description = stringResource(R.string.error_generic_desc),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        )
                    }

                    // Stagger: kartu muncul berurutan (delay bertingkat) agar
                    // daftar terasa hidup saat pertama kali dibuka.
                    is UiState.Success -> itemsIndexed(
                        items = state.data,
                        key = { _, report -> report.id }
                    ) { index, report ->
                        var visible by remember { mutableStateOf(false) }
                        val delayMillis = (index % 6) * 45
                        LaunchedEffect(Unit) { visible = true }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(260, delayMillis = delayMillis)) +
                                slideInVertically(
                                    tween(260, delayMillis = delayMillis)
                                ) { it / 5 }
                        ) {
                            ReportPhotoCard(
                                report = report,
                                onClick = { onReportClick(report.id) },
                                compact = true
                            )
                        }
                    }
                }
            }
        }

        if (tampilkanHasilFoto) {
            CariFotoSheet(
                sedangMencari = sedangMencariFoto,
                hasil = hasilCariFoto,
                galat = galatCariFoto,
                onReportClick = { id ->
                    tampilkanHasilFoto = false
                    viewModel.bersihkanHasilCariFoto()
                    onReportClick(id)
                },
                onDismiss = {
                    tampilkanHasilFoto = false
                    viewModel.bersihkanHasilCariFoto()
                }
            )
        }
    }
}

/**
 * Lembar hasil "cari barang dengan foto".
 *
 * Menampilkan laporan temuan berfoto yang paling mirip beserta persentase
 * kemiripan dari server. Selama model masih menghitung, pengguna melihat
 * indikator proses; bila tidak ada yang cocok, pesan kosong menjelaskan apa
 * yang bisa dicoba berikutnya (bukan galat merah, karena ini keadaan normal).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CariFotoSheet(
    sedangMencari: Boolean,
    hasil: List<HasilCariFoto>,
    galat: Boolean,
    onReportClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.lens_sheet_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.lens_sheet_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))

            when {
                sedangMencari -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.lens_loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                galat || hasil.isEmpty() -> EmptyState(
                    title = stringResource(
                        if (galat) R.string.lens_error_title else R.string.lens_empty_title
                    ),
                    description = stringResource(
                        if (galat) R.string.lens_error_desc else R.string.lens_empty_desc
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(hasil, key = { it.id }) { item ->
                        HasilFotoCard(
                            hasil = item,
                            onClick = { onReportClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

/** Satu kartu hasil pencarian foto: gambar, judul, kemiripan, dan lokasi pos. */
@Composable
private fun HasilFotoCard(
    hasil: HasilCariFoto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = hasil.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hasil.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.lens_similar, hasil.skor),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                hasil.posName?.takeIf { it.isNotBlank() }?.let { pos ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = pos,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedType: ItemType?,
    onTypeChange: (ItemType?) -> Unit,
    selectedCategory: ReportCategory?,
    onCategoryChange: (ReportCategory?) -> Unit,
    onCariFotoClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            placeholder = { Text(stringResource(R.string.home_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            // Ikon di kanan kotak pencarian: buka galeri lalu bandingkan fotonya
            // dengan laporan temuan berfoto (fitur "Lens").
            trailingIcon = {
                IconButton(onClick = onCariFotoClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cari_foto),
                        contentDescription = stringResource(R.string.home_search_photo)
                    )
                }
            }
        )

        // Kotak filter berdampingan dengan kotak pencarian.
        FilterBox(
            selectedType = selectedType,
            onTypeChange = onTypeChange,
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange
        )
    }
}

/**
 * Kotak filter di samping kotak pencarian.
 *
 * Menekannya membuka [ModalBottomSheet] berisi pilihan Jenis & Kategori —
 * lebih rapi daripada menu dropdown yang posisinya bergeser di tepi layar.
 * Saat ada filter aktif, kotak diberi warna aksen + titik penanda kecil.
 */
@Composable
private fun FilterBox(
    selectedType: ItemType?,
    onTypeChange: (ItemType?) -> Unit,
    selectedCategory: ReportCategory?,
    onCategoryChange: (ReportCategory?) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val active = selectedType != null || selectedCategory != null

    Surface(
        onClick = { showSheet = true },
        modifier = Modifier.size(56.dp),
        shape = MaterialTheme.shapes.small,
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_filter),
                contentDescription = stringResource(R.string.home_filter_title),
                modifier = Modifier.size(22.dp)
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }

    if (showSheet) {
        FilterSheet(
            selectedType = selectedType,
            onTypeChange = onTypeChange,
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange,
            onDismiss = { showSheet = false }
        )
    }
}

/** Lembar filter (bottom sheet) berisi pilihan Jenis & Kategori. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    selectedType: ItemType?,
    onTypeChange: (ItemType?) -> Unit,
    selectedCategory: ReportCategory?,
    onCategoryChange: (ReportCategory?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.home_filter_title),
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(
                    onClick = {
                        onTypeChange(null)
                        onCategoryChange(null)
                    }
                ) {
                    Text(text = stringResource(R.string.home_filter_reset))
                }
            }

            FilterGroup(title = stringResource(R.string.home_filter_type)) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onTypeChange(null) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = selectedType == ItemType.LOST,
                    onClick = { onTypeChange(ItemType.LOST) },
                    label = { Text(stringResource(R.string.filter_lost)) }
                )
                FilterChip(
                    selected = selectedType == ItemType.FOUND,
                    onClick = { onTypeChange(ItemType.FOUND) },
                    label = { Text(stringResource(R.string.filter_found)) }
                )
            }

            FilterGroup(title = stringResource(R.string.home_filter_category)) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text(stringResource(R.string.home_filter_category_all)) }
                )
                ReportCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category.label()) }
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.home_filter_apply))
            }
        }
    }
}

/** Kelompok pilihan pada lembar filter: judul + chip yang otomatis membungkus. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}

/** Lonceng di top bar Beranda + badge jumlah hal baru. */
@Composable
private fun NotificationButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.home_notification_bell)
            )
        }

        if (count > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }
}

/**
 * Kartu sapaan — kotak paling atas pada Beranda.
 *
 * Berisi identitas pengguna, tanggal hari ini, dan tiga angka ringkas. Latar
 * solid + ornamen lingkaran lembut + elevasi memberi kesan kedalaman (depth)
 * tanpa efek 3D yang berlebihan dan tetap aman untuk mode terang/gelap.
 */
@Composable
private fun HomeGreetingCard(
    userName: String,
    stats: HomeStats,
    modifier: Modifier = Modifier
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Ornamen lingkaran lembut -> kesan kedalaman.
                    drawCircle(
                        color = onPrimary.copy(alpha = 0.10f),
                        radius = size.minDimension * 0.55f,
                        center = Offset(size.width * 1.02f, size.height * 0.05f)
                    )
                    drawCircle(
                        color = onPrimary.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.38f,
                        center = Offset(size.width * 0.62f, size.height * 1.05f)
                    )
                }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InitialsAvatar(
                    name = userName,
                    size = 54.dp,
                    containerColor = onPrimary.copy(alpha = 0.22f),
                    contentColor = onPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(
                            R.string.home_greeting,
                            userName.substringBefore(' ')
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = onPrimary
                    )
                    Text(
                        text = todayLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    value = stats.total,
                    label = stringResource(R.string.home_stat_total),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = stats.readyForPickup,
                    label = stringResource(R.string.home_stat_ready),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = stats.pendingSync,
                    label = stringResource(R.string.home_stat_pending),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Satu angka ringkas pada kartu sapaan. */
@Composable
private fun StatItem(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

/** "Sabtu, 14 Oktober 2026" mengikuti tanggal perangkat. */
private fun todayLabel(): String =
    LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"))
    )

/** Kotak pengumuman (dummy) — mis. jam layanan pos barang temuan. */
@Composable
private fun AnnouncementCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.home_announcement_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(R.string.home_announcement_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(R.string.home_announcement_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
