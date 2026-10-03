package org.umn.application_ketemu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportStatus

/**
 * Timeline "Perjalanan Barang" — inti fitur Dashboard Tracking pada PRD.
 *
 * Aturan tampilan:
 *  - tahap yang sudah dilewati ditandai bulatan berwarna,
 *  - tahap aktif memakai warna status saat ini,
 *  - tahap yang belum terjadi tetap ditampilkan (agar pengguna tahu alurnya)
 *    namun diberi label "Menunggu" dan warna netral.
 *
 * Sengaja tidak menampilkan tanggal palsu untuk tahap yang belum terjadi —
 * hanya tahap "Dilaporkan" yang memiliki waktu nyata dari data.
 */
@Composable
fun StatusTimeline(
    report: Report,
    modifier: Modifier = Modifier
) {
    val accent = reportStatusColor(report.status)

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = stringResource(R.string.report_timeline_title),
            style = MaterialTheme.typography.titleMedium
        )

        Column(modifier = Modifier.padding(top = 12.dp)) {
            ReportStatus.entries.forEachIndexed { index, status ->
                val reached = report.status.step >= status.step
                TimelineStep(
                    title = stringResource(reportStatusLabel(status)),
                    subtitle = when {
                        !reached -> stringResource(R.string.report_timeline_waiting)
                        status == ReportStatus.DILAPORKAN ->
                            RelativeTime.format(report.createdAt)

                        else -> stringResource(R.string.report_timeline_recorded)
                    },
                    reached = reached,
                    accent = accent,
                    isLast = index == ReportStatus.entries.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TimelineStep(
    title: String,
    subtitle: String,
    reached: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    isLast: Boolean
) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (reached) accent else MaterialTheme.colorScheme.outlineVariant
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            if (reached) accent.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Column(modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (reached) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
