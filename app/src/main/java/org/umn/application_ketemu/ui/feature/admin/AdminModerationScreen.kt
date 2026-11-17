package org.umn.application_ketemu.ui.feature.admin

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.PosCatalog
import org.umn.application_ketemu.domain.model.PosLocation
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportCategory
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.ui.components.label
import org.umn.application_ketemu.ui.components.reportStatusLabel

/**
 * Antrean moderasi: laporan baru yang belum diverifikasi petugas.
 *
 * Aksi utama membuka dialog serah-terima: satpam memilih Pos tujuan yang
 * benar-benar menerima barang, lalu status naik ke [ReportStatus.DISERAHKAN_KE_POS].
 * Aksi kedua mengarsipkan laporan yang tidak relevan.
 *
 * Tampilan mengikuti referensi desain ruang admin: header besar, satu kartu
 * ringkasan mint ("perlu ditinjau"), lalu kartu laporan putih berisi ikon
 * kategori, identitas pelapor, lokasi terakhir, dan dua tombol keputusan.
 */
@Composable
fun AdminModerationScreen(
    viewModel: AdminModerationViewModel,
    onBack: () -> Unit
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val allowed by viewModel.isAllowed.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Laporan yang menunggu konfirmasi serah-terima. Dialog baru muncul setelah
    // satpam menekan "Verifikasi", sehingga tidak ada status yang berubah tanpa
    // persetujuan eksplisit.
    var handoverTarget by remember { mutableStateOf<Report?>(null) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.clearMessage()
    }

    // Pengguna tanpa hak tetap melihat kerangka panel + pesan penolakan.
    if (!allowed) {
        AdminScaffold(
            title = stringResource(R.string.admin_menu_moderation),
            onBack = onBack
        ) { padding ->
            AdminAccessDenied(modifier = Modifier.padding(padding))
        }
        return
    }

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
                        title = stringResource(R.string.admin_menu_moderation),
                        subtitle = stringResource(R.string.admin_moderation_subtitle),
                        onBack = onBack
                    )
                }
            }

            item(key = "summary") {
                AdminReveal(index = 1) {
                    ModerationSummaryCard(
                        count = queue.size,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item(key = "section") {
                AdminReveal(index = 2) {
                    AdminSectionHeader(
                        title = stringResource(R.string.admin_moderation_queue_section),
                        modifier = Modifier.padding(top = 8.dp),
                        trailing = {
                            Text(
                                // Jumlah pada judul bagian memakai sumber data yang
                                // sama dengan kartu ringkasan, jadi selalu cocok.
                                text = stringResource(
                                    R.string.admin_moderation_queue_count,
                                    queue.size
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (queue.isEmpty()) {
                item(key = "empty") {
                    AdminReveal(index = 3) { ModerationEmptyCard() }
                }
            } else {
                itemsIndexed(items = queue, key = { _, report -> report.id }) { index, report ->
                    AdminReveal(index = index + 3, modifier = Modifier.animateItem()) {
                        ModerationCard(
                            report = report,
                            onVerify = { handoverTarget = report },
                            onArchive = { viewModel.archive(report.id) }
                        )
                    }
                }
            }

            item(key = "hint") {
                AdminReveal(index = queue.size + 3) {
                    AdminHintRow(
                        text = stringResource(R.string.admin_moderation_hint),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    // Dialog konfirmasi: satpam memilih pos tujuan, baru status laporan berubah.
    handoverTarget?.let { report ->
        HandoverDialog(
            report = report,
            onDismiss = { handoverTarget = null },
            onConfirm = { posId ->
                handoverTarget = null
                viewModel.verifyAndForward(report.id, posId)
            }
        )
    }
}

/**
 * Kartu ringkasan antrean: label "perlu ditinjau", angka besar, dan ikon
 * perisai di kotak putih sebagai penanda tugas verifikasi.
 *
 * Bentuknya memakai [AdminSummaryCard] bersama agar gradasi mint dan susunan
 * angkanya identik dengan kartu ringkasan layar admin lain.
 */
@Composable
private fun ModerationSummaryCard(
    count: Int,
    modifier: Modifier = Modifier
) {
    AdminSummaryCard(
        label = stringResource(R.string.admin_moderation_pending_label),
        count = count,
        unit = stringResource(R.string.admin_moderation_pending_unit),
        iconRes = R.drawable.ic_admin_moderation,
        tone = rememberAdminTones().mint,
        modifier = modifier
    )
}

/**
 * Satu laporan pada antrean.
 *
 * Urutan bacaan mengikuti keputusan moderator: status & waktu masuk -> barang
 * apa -> siapa pelapornya -> di mana terakhir terlihat -> aksi.
 */
@Composable
private fun ModerationCard(
    report: Report,
    onVerify: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryTone = adminCategoryTone(report.category, rememberAdminTones())

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
                ModerationStatusChip(status = report.status)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = RelativeTime.format(report.createdAt),
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
                        .background(categoryTone.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = adminCategoryIcon(report.category)),
                        contentDescription = null,
                        tint = categoryTone.content,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = report.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ReportMetaRow(icon = Icons.Default.Person, text = report.ownerName)
            ReportMetaRow(icon = Icons.Default.Place, text = report.lastSeenLocation)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onVerify,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    // Label panjang ("Verifikasi & teruskan ke Pos") dibatasi satu
                    // baris agar tinggi kedua tombol tetap sejajar.
                    Text(
                        text = stringResource(R.string.admin_action_verify),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onArchive,
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.admin_action_archive),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Baris metadata berikon (pelapor / lokasi) di dalam kartu laporan. */
@Composable
private fun ReportMetaRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Dialog konfirmasi serah-terima.
 *
 * Satpam melihat ringkasan barang lalu memilih pos tempat barang benar-benar
 * dititipkan. Pos sengaja dipilih di sini (bukan oleh pelapor) karena hanya
 * petugas yang tahu ke pos mana barang akhirnya dibawa.
 *
 * @param onConfirm menerima id pos terpilih; dipanggil hanya setelah satpam
 *   menekan tombol konfirmasi.
 */
@Composable
private fun HandoverDialog(
    report: Report,
    onDismiss: () -> Unit,
    onConfirm: (posId: String) -> Unit
) {
    val tones = rememberAdminTones()
    val categoryTone = adminCategoryTone(report.category, tones)
    // Pos yang sudah tercatat (mis. laporan temuan lama) jadi pilihan awal;
    // kalau belum ada, pakai pos pertama sebagai default netral.
    var selectedPosId by rememberSaveable(report.id) {
        mutableStateOf(report.pos?.id ?: PosCatalog.default.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.admin_handover_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(categoryTone.container),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = adminCategoryIcon(report.category)),
                            contentDescription = null,
                            tint = categoryTone.content,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = report.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                if (report.type == ItemType.LOST) {
                                    R.string.filter_lost
                                } else {
                                    R.string.filter_found
                                }
                            ) + " · " + report.category.label(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ReportMetaRow(icon = Icons.Default.Person, text = report.ownerName)
                ReportMetaRow(icon = Icons.Default.Place, text = report.lastSeenLocation)

                Text(
                    text = stringResource(R.string.admin_handover_pos_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosCatalog.all.forEach { pos ->
                        PosOptionRow(
                            pos = pos,
                            selected = pos.id == selectedPosId,
                            onClick = { selectedPosId = pos.id }
                        )
                    }
                }

                AdminHintRow(text = stringResource(R.string.admin_handover_hint))
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPosId) }) {
                Text(text = stringResource(R.string.admin_handover_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.admin_handover_cancel))
            }
        }
    )
}

/** Satu pilihan pos pada dialog; bentuknya mengikuti tone pastel panel admin. */
@Composable
private fun PosOptionRow(
    pos: PosLocation,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tone = rememberAdminTones().mint

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) tone.container else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) tone.content else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) tone.content else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (selected) tone.content else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = tone.container,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pos.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) tone.content else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pos.building,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Chip status pada kartu antrean.
 *
 * Warna amber dipakai tetap (bukan warna status umum) karena antrean moderasi
 * hanya berisi laporan berstatus "Dilaporkan"; labelnya tetap diambil dari
 * [reportStatusLabel] agar teks status tidak diduplikasi di sini.
 */
@Composable
private fun ModerationStatusChip(status: ReportStatus) {
    val tone = rememberAdminTones().amber

    Surface(
        shape = MaterialTheme.shapes.small,
        color = tone.container
    ) {
        Text(
            text = stringResource(reportStatusLabel(status)),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tone.content
        )
    }
}

/**
 * Keadaan "antrean bersih".
 *
 * Header dan kartu ringkasan tetap tampil supaya pengguna tahu layar ini
 * memang kosong, bukan gagal memuat.
 */
@Composable
private fun ModerationEmptyCard(modifier: Modifier = Modifier) {
    val tone = rememberAdminTones().slate

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(tone.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_admin_clipboard_check),
                    contentDescription = null,
                    tint = tone.content,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.admin_queue_empty),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.admin_queue_empty_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Ikon kategori barang; dipakai sebagai penanda visual cepat pada kartu. */
@DrawableRes
private fun adminCategoryIcon(category: ReportCategory): Int = when (category) {
    ReportCategory.ELEKTRONIK -> R.drawable.ic_admin_device
    ReportCategory.DOKUMEN -> R.drawable.ic_admin_document
    ReportCategory.AKSESORIS -> R.drawable.ic_admin_bag
    ReportCategory.LAINNYA -> R.drawable.ic_admin_archive
}

/** Tone ikon per kategori, dipilih dari palet pastel panel admin. */
private fun adminCategoryTone(category: ReportCategory, tones: AdminTones): AdminTone =
    when (category) {
        ReportCategory.ELEKTRONIK -> tones.blue
        ReportCategory.DOKUMEN -> tones.amber
        ReportCategory.AKSESORIS -> tones.violet
        ReportCategory.LAINNYA -> tones.slate
    }

