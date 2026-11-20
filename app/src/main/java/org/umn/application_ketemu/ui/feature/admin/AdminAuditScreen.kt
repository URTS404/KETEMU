package org.umn.application_ketemu.ui.feature.admin

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.AuditAction
import org.umn.application_ketemu.domain.model.AuditLogEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Log aktivitas moderator: riwayat keputusan, terbaru di atas, tidak bisa diubah.
 *
 * Tampilan mengikuti bahasa desain ruang admin: header besar, satu kartu
 * ringkasan biru berisi total catatan, saringan cepat per jenis target, lalu
 * riwayat yang dikelompokkan per tanggal. Pengelompokan ini membuat keputusan
 * "hari ini" mudah ditemukan tanpa membaca seluruh daftar, sementara jejak lama
 * tetap utuh sebagai bukti audit.
 */
@Composable
fun AdminAuditScreen(
    viewModel: AdminAuditViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val allowed by viewModel.isAllowed.collectAsStateWithLifecycle()

    // Pengguna tanpa hak tetap melihat kerangka panel + pesan penolakan.
    if (!allowed) {
        AdminScaffold(
            title = stringResource(R.string.admin_menu_audit),
            onBack = onBack
        ) { padding ->
            AdminAccessDenied(modifier = Modifier.padding(padding))
        }
        return
    }

    // Saringan disimpan di layar (bukan ViewModel) karena hanya memengaruhi
    // daftar yang sudah dimuat; sumber datanya tetap utuh dan append-only.
    var filter by rememberSaveable { mutableStateOf(AuditTargetFilter.ALL) }

    val visibleEntries = remember(entries, filter) {
        entries.filter { filter.matches(it.targetType) }
    }
    val dayGroups = remember(visibleEntries) { groupAuditByDate(visibleEntries) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
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
                        title = stringResource(R.string.admin_menu_audit),
                        subtitle = stringResource(R.string.admin_audit_subtitle),
                        onBack = onBack
                    )
                }
            }

            item(key = "summary") {
                AdminReveal(index = 1) {
                    AuditSummaryCard(
                        count = entries.size,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Saringan baru berguna saat ada catatan; log kosong cukup menampilkan
            // kartu penjelasan tanpa chip yang tidak mengubah apa pun.
            if (entries.isNotEmpty()) {
                item(key = "filter") {
                    AdminReveal(index = 2) {
                        AuditFilterRow(
                            selected = filter,
                            onSelect = { filter = it }
                        )
                    }
                }
            }

            item(key = "section") {
                AdminReveal(index = 3) {
                    AdminSectionHeader(
                        title = stringResource(R.string.admin_audit_history_section),
                        trailing = {
                            Text(
                                // Jumlah mengikuti hasil saringan, sama seperti
                                // daftar yang tampil di bawahnya.
                                text = stringResource(R.string.admin_audit_count, visibleEntries.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (visibleEntries.isEmpty()) {
                item(key = "empty") {
                    AdminReveal(index = 4) {
                        AuditEmptyCard(isFiltered = entries.isNotEmpty())
                    }
                }
            } else {
                // Indeks reveal berjalan terus antar kelompok hari supaya
                // gelombangnya tetap terasa turun dari atas ke bawah.
                var revealIndex = 4
                dayGroups.forEach { group ->
                    val dayIndex = revealIndex
                    item(key = "day-${group.date}") {
                        AdminReveal(index = dayIndex) {
                            AuditDayLabel(
                                date = group.date,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    itemsIndexed(
                        items = group.entries,
                        key = { _, entry -> entry.id }
                    ) { index, entry ->
                        AdminReveal(
                            index = dayIndex + 1 + index,
                            modifier = Modifier.animateItem()
                        ) {
                            AuditEntryCard(entry = entry)
                        }
                    }
                    revealIndex = dayIndex + 1 + group.entries.size
                }
            }

            item(key = "hint") {
                AdminReveal(index = visibleEntries.size + 4) {
                    AdminHintRow(
                        text = stringResource(R.string.admin_audit_hint),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Kartu ringkasan log: jumlah catatan yang tersimpan. Gradasinya biru — sama
 * dengan ikon menu "Log aktivitas" di dashboard — agar bagian ini mudah dikenali.
 */
@Composable
private fun AuditSummaryCard(
    count: Int,
    modifier: Modifier = Modifier
) {
    AdminSummaryCard(
        label = stringResource(R.string.admin_audit_total_label),
        count = count,
        unit = stringResource(R.string.admin_audit_total_unit),
        iconRes = R.drawable.ic_admin_history,
        tone = rememberAdminTones().blue,
        modifier = modifier
    )
}

/**
 * Saringan cepat berdasarkan jenis target keputusan.
 *
 * Hanya tiga pilihan agar tetap terbaca dalam satu baris di layar sempit; jenis
 * aksinya (verifikasi, arsip, setujui, tolak) sudah terlihat pada tiap kartu.
 */
private enum class AuditTargetFilter(
    @StringRes val labelRes: Int,
    private val targetType: String?
) {
    ALL(R.string.admin_audit_filter_all, null),
    REPORT(R.string.admin_audit_filter_report, TARGET_TYPE_REPORT),
    CLAIM(R.string.admin_audit_filter_claim, TARGET_TYPE_CLAIM);

    fun matches(value: String?): Boolean = targetType == null || targetType == value
}

/** Chip saringan; chip aktif memakai pastel biru agar jelas sedang dipakai. */
@Composable
private fun AuditFilterRow(
    selected: AuditTargetFilter,
    onSelect: (AuditTargetFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val tone = rememberAdminTones().blue

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AuditTargetFilter.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tone.container,
                    selectedLabelColor = tone.content
                ),
                label = {
                    Text(
                        text = stringResource(option.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    }
}

/** Judul kelompok tanggal: "Hari ini", "Kemarin", atau tanggal lengkap. */
@Composable
private fun AuditDayLabel(date: LocalDate, modifier: Modifier = Modifier) {
    Text(
        text = auditDayLabel(date),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun auditDayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.admin_audit_today)
        today.minusDays(1) -> stringResource(R.string.admin_audit_yesterday)
        else -> date.format(AuditDateFormatter)
    }
}


/**
 * Satu catatan audit: ikon aksi berwarna, jenis keputusan, objek yang diputuskan,
 * lalu pelaku dan waktunya. Tiap jenis aksi punya warna sendiri sehingga riwayat
 * bisa dipindai hanya dari warnanya.
 */
@Composable
private fun AuditEntryCard(entry: AuditLogEntry, modifier: Modifier = Modifier) {
    val tone = auditActionTone(entry.action)
    val note = entry.note?.takeIf { it.isNotBlank() }

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
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(tone.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = auditActionIcon(entry.action)),
                    contentDescription = null,
                    tint = tone.content,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = auditActionLabel(entry.action),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = RelativeTime.format(entry.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (note != null) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AuditActorAvatar(name = entry.actorName)
                    Text(
                        text = entry.actorName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AuditTargetChip(targetType = entry.targetType)
                }
            }
        }
    }
}

/** Inisial pelaku dalam lingkaran kecil, pengganti foto profil yang belum ada. */
@Composable
private fun AuditActorAvatar(name: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Label kecil "Laporan"/"Klaim" agar objek keputusan tidak ambigu. */
@Composable
private fun AuditTargetChip(targetType: String) {
    val labelRes = if (targetType == TARGET_TYPE_CLAIM) {
        R.string.admin_audit_filter_claim
    } else {
        R.string.admin_audit_filter_report
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Keadaan kosong di dalam daftar (bukan layar penuh) supaya header dan kartu
 * ringkasan tetap terlihat: pengguna tahu log memang kosong, bukan layar gagal.
 *
 * @param isFiltered `true` bila log berisi data tetapi hasil saringan kosong
 */
@Composable
private fun AuditEmptyCard(isFiltered: Boolean, modifier: Modifier = Modifier) {
    val tone = rememberAdminTones().slate

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tone.container)
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
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_admin_history),
                    contentDescription = null,
                    tint = tone.content,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        if (isFiltered) R.string.admin_audit_filter_empty else R.string.admin_audit_empty
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tone.content
                )
                Text(
                    text = stringResource(
                        if (isFiltered) {
                            R.string.admin_audit_filter_empty_desc
                        } else {
                            R.string.admin_audit_empty_desc
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = tone.content
                )
            }
        }
    }
}

/**
 * Warna ikon per aksi. "Tolak klaim" memakai warna error bawaan tema supaya
 * keputusan negatif langsung terbaca, sedangkan aksi lain memakai pastel admin.
 */
@Composable
private fun auditActionTone(action: AuditAction): AdminTone {
    val tones = rememberAdminTones()
    return when (action) {
        AuditAction.VERIFIKASI_LAPORAN -> tones.mint
        AuditAction.ARSIP_LAPORAN -> tones.amber
        AuditAction.BUKA_ARSIP_LAPORAN -> tones.blue
        AuditAction.SETUJUI_KLAIM -> tones.violet
        // Serah terima menutup alur klaim, jadi memakai warna hijau "selesai".
        AuditAction.SERAH_TERIMA_BARANG -> tones.mint
        AuditAction.TOLAK_KLAIM -> AdminTone(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
        // Aksi admin pada akun: ungu, sama dengan menu Pengguna.
        AuditAction.UBAH_PERAN,
        AuditAction.VERIFIKASI_AKUN,
        AuditAction.BLOKIR_AKUN,
        AuditAction.BUKA_BLOKIR_AKUN -> tones.violet
    }
}

@DrawableRes
private fun auditActionIcon(action: AuditAction): Int = when (action) {
    AuditAction.VERIFIKASI_LAPORAN -> R.drawable.ic_admin_clipboard_check
    AuditAction.ARSIP_LAPORAN -> R.drawable.ic_admin_archive
    AuditAction.BUKA_ARSIP_LAPORAN -> R.drawable.ic_admin_unarchive
    AuditAction.SETUJUI_KLAIM -> R.drawable.ic_admin_check_circle
    AuditAction.SERAH_TERIMA_BARANG -> R.drawable.ic_admin_bag
    AuditAction.TOLAK_KLAIM -> R.drawable.ic_admin_cancel
    AuditAction.UBAH_PERAN,
    AuditAction.VERIFIKASI_AKUN,
    AuditAction.BLOKIR_AKUN,
    AuditAction.BUKA_BLOKIR_AKUN -> R.drawable.ic_admin_users
}

/** Satu kelompok riwayat untuk satu tanggal. */
private data class AuditDayGroup(val date: LocalDate, val entries: List<AuditLogEntry>)

/**
 * Mengelompokkan catatan per tanggal setempat, tanggal terbaru di atas.
 *
 * Daftar sumber sudah terurut terbaru-dulu, tetapi pengurutan diulang di sini
 * agar tampilan tidak bergantung pada urutan dari repository.
 */
private fun groupAuditByDate(entries: List<AuditLogEntry>): List<AuditDayGroup> {
    val zone = ZoneId.systemDefault()
    return entries
        .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
        .toSortedMap(compareByDescending<LocalDate> { it })
        .map { (date, dayEntries) ->
            AuditDayGroup(date = date, entries = dayEntries.sortedByDescending { it.createdAt })
        }
}

private const val TARGET_TYPE_REPORT = "REPORT"
private const val TARGET_TYPE_CLAIM = "CLAIM"

private val AuditDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))

