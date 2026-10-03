package org.umn.application_ketemu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.umn.application_ketemu.R
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.SyncStatus
import org.umn.application_ketemu.ui.theme.StatusAtPosDark
import org.umn.application_ketemu.ui.theme.StatusAtPosLight
import org.umn.application_ketemu.ui.theme.StatusDoneDark
import org.umn.application_ketemu.ui.theme.StatusDoneLight
import org.umn.application_ketemu.ui.theme.StatusPendingSyncDark
import org.umn.application_ketemu.ui.theme.StatusPendingSyncLight
import org.umn.application_ketemu.ui.theme.StatusReadyDark
import org.umn.application_ketemu.ui.theme.StatusReadyLight
import org.umn.application_ketemu.ui.theme.StatusReportedDark
import org.umn.application_ketemu.ui.theme.StatusReportedLight

/**
 * Chip status barang.
 *
 * Warna latar memakai warna status dengan alpha rendah di atas permukaan,
 * sehingga tetap terbaca & kontras yang layak pada tema terang maupun gelap
 * tanpa menduplikasi daftar warna untuk tiap tema.
 */
@Composable
fun ReportStatusChip(
    status: ReportStatus,
    modifier: Modifier = Modifier
) {
    val accent = reportStatusColor(status)
    TagSurface(accent = accent, modifier = modifier) {
        Text(
            text = stringResource(reportStatusLabel(status)),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}

/**
 * Chip tambahan khusus fitur offline-first.
 * Hanya muncul untuk laporan PENDING/FAILED supaya pengguna tahu bahwa
 * laporannya belum sampai ke server (bukan hilang).
 */
@Composable
fun SyncStatusChip(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    if (syncStatus == SyncStatus.SYNCED) return

    val accent = if (syncStatus == SyncStatus.PENDING) {
        if (isSystemInDarkTheme()) StatusPendingSyncDark else StatusPendingSyncLight
    } else {
        MaterialTheme.colorScheme.error
    }

    TagSurface(accent = accent, modifier = modifier) {
        Text(
            text = stringResource(
                if (syncStatus == SyncStatus.PENDING) R.string.status_pending_sync
                else R.string.status_sync_failed
            ),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}

@Composable
private fun TagSurface(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            content()
        }
    }
}

/** Pemetaan status -> warna, mengikuti palet status pada ui/theme/Color.kt. */
@Composable
fun reportStatusColor(status: ReportStatus): Color {
    val dark = isSystemInDarkTheme()
    return when (status) {
        ReportStatus.DILAPORKAN ->
            if (dark) StatusReportedDark else StatusReportedLight

        ReportStatus.DISERAHKAN_KE_POS ->
            if (dark) StatusAtPosDark else StatusAtPosLight

        ReportStatus.SIAP_DIAMBIL ->
            if (dark) StatusReadyDark else StatusReadyLight

        ReportStatus.SELESAI_DIKLAIM ->
            if (dark) StatusDoneDark else StatusDoneLight
    }
}

/** Pemetaan status -> string resource (UI selalu berbahasa Indonesia). */
fun reportStatusLabel(status: ReportStatus): Int = when (status) {
    ReportStatus.DILAPORKAN -> R.string.status_reported
    ReportStatus.DISERAHKAN_KE_POS -> R.string.status_handed_to_pos
    ReportStatus.SIAP_DIAMBIL -> R.string.status_ready_for_pickup
    ReportStatus.SELESAI_DIKLAIM -> R.string.status_claimed_done
}

/** Baris chip yang dipakai bersama oleh kartu & halaman detail. */
@Composable
fun StatusChipRow(
    status: ReportStatus,
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        ReportStatusChip(status = status)
        if (syncStatus != SyncStatus.SYNCED) {
            Box(modifier = Modifier.padding(start = 6.dp)) {
                SyncStatusChip(syncStatus = syncStatus)
            }
        }
    }
}
