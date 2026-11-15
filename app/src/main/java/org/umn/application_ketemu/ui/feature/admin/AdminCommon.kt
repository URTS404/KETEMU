package org.umn.application_ketemu.ui.feature.admin

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.AuditAction
import org.umn.application_ketemu.domain.model.AuditLogEntry
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.theme.AdminToneAmberContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneAmberContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneAmberContentDark
import org.umn.application_ketemu.ui.theme.AdminToneAmberContentLight
import org.umn.application_ketemu.ui.theme.AdminToneBlueContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneBlueContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneBlueContentDark
import org.umn.application_ketemu.ui.theme.AdminToneBlueContentLight
import org.umn.application_ketemu.ui.theme.AdminToneMintContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneMintContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneMintContentDark
import org.umn.application_ketemu.ui.theme.AdminToneMintContentLight
import org.umn.application_ketemu.ui.theme.AdminToneSlateContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneSlateContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneSlateContentDark
import org.umn.application_ketemu.ui.theme.AdminToneSlateContentLight
import org.umn.application_ketemu.ui.theme.AdminToneVioletContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneVioletContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneVioletContentDark
import org.umn.application_ketemu.ui.theme.AdminToneVioletContentLight

/**
 * Kerangka layar panel admin.
 *
 * Semua layar admin memakai header yang sama (judul + tombol kembali) agar
 * terasa satu modul, bukan kumpulan layar terpisah. Snackbar opsional dipakai
 * layar yang punya aksi tulis (moderasi & klaim).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        content = content
    )
}

/**
 * Pasangan warna pastel untuk satu kartu panel admin.
 *
 * @param container latar kartu (pastel lembut)
 * @param content warna angka, judul kecil, dan ikon di atas container
 */
data class AdminTone(val container: Color, val content: Color)

/** Lima tone yang dipakai dashboard admin. */
data class AdminTones(
    val mint: AdminTone,
    val amber: AdminTone,
    val slate: AdminTone,
    val blue: AdminTone,
    val violet: AdminTone
)

private val LightAdminTones = AdminTones(
    mint = AdminTone(AdminToneMintContainerLight, AdminToneMintContentLight),
    amber = AdminTone(AdminToneAmberContainerLight, AdminToneAmberContentLight),
    slate = AdminTone(AdminToneSlateContainerLight, AdminToneSlateContentLight),
    blue = AdminTone(AdminToneBlueContainerLight, AdminToneBlueContentLight),
    violet = AdminTone(AdminToneVioletContainerLight, AdminToneVioletContentLight)
)

private val DarkAdminTones = AdminTones(
    mint = AdminTone(AdminToneMintContainerDark, AdminToneMintContentDark),
    amber = AdminTone(AdminToneAmberContainerDark, AdminToneAmberContentDark),
    slate = AdminTone(AdminToneSlateContainerDark, AdminToneSlateContentDark),
    blue = AdminTone(AdminToneBlueContainerDark, AdminToneBlueContentDark),
    violet = AdminTone(AdminToneVioletContainerDark, AdminToneVioletContentDark)
)

/**
 * Memilih tone sesuai tema aktif.
 *
 * Warna pastel harus ikut berubah saat mode gelap menyala, karena tone terang
 * (mis. mint #DDF3E9) akan menyilaukan di latar #0E1515.
 */
@Composable
fun rememberAdminTones(): AdminTones =
    if (isSystemInDarkTheme()) DarkAdminTones else LightAdminTones

/**
 * Header layar panel admin: tombol kembali, label modul, judul besar, dan satu
 * baris penjelasan.
 *
 * Dashboard & moderasi memakai header ini alih-alih [TopAppBar] agar judulnya
 * besar dan terasa seperti beranda kerja, bukan layar biasa. Chip peran hanya
 * muncul bila [roleLabel] diisi — layar kerja tidak perlu menegaskan peran lagi.
 *
 * @param title judul besar layar, mis. "Moderasi laporan"
 * @param subtitle satu baris penjelas tugas layar tersebut
 * @param roleLabel nama peran aktif; `null` menyembunyikan chip peran
 */
@Composable
fun AdminScreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    roleLabel: String? = null
) {
    val tones = rememberAdminTones()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.admin_module_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (roleLabel != null) {
                Surface(
                    shape = CircleShape,
                    color = tones.mint.container
                ) {
                    Text(
                        text = roleLabel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tones.mint.content
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Judul bagian pada dashboard ("Ringkasan", "Pengelolaan").
 *
 * @param trailing slot opsional di sisi kanan, mis. keterangan cakupan angka.
 */
@Composable
fun AdminSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

/**
 * Kartu angka ringkas pada dashboard admin.
 *
 * Susunannya mengikuti referensi desain: label di atas, angka besar di kiri
 * bawah, dan ikon di kotak kecil sebelah kanan. Label diberi `minLines = 2`
 * supaya keempat kartu selalu sama tinggi meski panjang labelnya berbeda.
 */
@Composable
fun AdminStatCard(
    count: Int,
    label: String,
    @DrawableRes iconRes: Int,
    tone: AdminTone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tone.container)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 124.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tone.content,
                minLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = tone.content
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(tone.content.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = tone.content,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Baris menu navigasi pada dashboard admin: ikon berwarna, judul + penjelasan,
 * lalu chevron sebagai penanda bahwa baris bisa diketuk.
 */
@Composable
fun AdminMenuRow(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    tone: AdminTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(tone.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = tone.content,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Nama peran berbahasa Indonesia untuk chip peran ("Satpam"/"Admin").
 *
 * Enum [UserRole] sengaja tidak ditampilkan apa adanya karena pengguna aplikasi
 * melihat istilah Indonesia pada seluruh layar lain.
 */
@Composable
fun adminRoleLabel(role: UserRole): String = stringResource(
    if (role == UserRole.ADMIN) R.string.admin_role_admin else R.string.admin_role_staff
)

/** Keadaan "tidak berhak" yang dipakai semua layar admin. */
@Composable
fun AdminAccessDenied(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.admin_access_denied_title),
        description = stringResource(R.string.admin_access_denied_desc),
        modifier = modifier.fillMaxSize()
    )
}

/** Label Bahasa Indonesia untuk setiap jenis aksi audit. */
@Composable
fun auditActionLabel(action: AuditAction): String = stringResource(
    when (action) {
        AuditAction.VERIFIKASI_LAPORAN -> R.string.admin_audit_action_verify
        AuditAction.ARSIP_LAPORAN -> R.string.admin_audit_action_archive
        AuditAction.BUKA_ARSIP_LAPORAN -> R.string.admin_audit_action_unarchive
        AuditAction.SETUJUI_KLAIM -> R.string.admin_audit_action_approve_claim
        AuditAction.TOLAK_KLAIM -> R.string.admin_audit_action_reject_claim
    }
)

/** Satu baris log aktivitas (dipakai dashboard & layar log penuh). */
@Composable
fun AuditRow(entry: AuditLogEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = entry.actorName.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = auditActionLabel(entry.action),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${entry.actorName} · ${entry.note.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = RelativeTime.format(entry.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
