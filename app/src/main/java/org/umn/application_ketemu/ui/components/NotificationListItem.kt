package org.umn.application_ketemu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.AppNotification
import org.umn.application_ketemu.domain.model.AppNotificationType

/**
 * Ikon per jenis notifikasi.
 *
 * Hanya memakai ikon Material bawaan (material-icons-core) supaya ukuran APK
 * tetap kecil. Dipakai bersama oleh lembar Beranda dan layar Notifikasi agar
 * satu jenis selalu tampil dengan ikon yang sama.
 */
fun notificationIcon(type: AppNotificationType): ImageVector = when (type) {
    AppNotificationType.LOST_REPORT_CREATED,
    AppNotificationType.FOUND_REPORT_CREATED,
    AppNotificationType.CLAIM_APPROVED -> Icons.Default.Check

    AppNotificationType.NEW_FOUND_ITEM -> Icons.Default.Search
    AppNotificationType.POSSIBLE_MATCH -> Icons.Default.Star
    AppNotificationType.REPORT_STATUS_CHANGED -> Icons.Default.Refresh
    AppNotificationType.CLAIM_REJECTED -> Icons.Default.Close
}

/** Warna aksen per jenis notifikasi; memakai token tema agar aman di mode gelap. */
@Composable
fun notificationAccent(type: AppNotificationType): Color = when (type) {
    AppNotificationType.LOST_REPORT_CREATED,
    AppNotificationType.FOUND_REPORT_CREATED,
    AppNotificationType.REPORT_STATUS_CHANGED -> MaterialTheme.colorScheme.primary

    AppNotificationType.NEW_FOUND_ITEM,
    AppNotificationType.POSSIBLE_MATCH,
    AppNotificationType.CLAIM_APPROVED -> MaterialTheme.colorScheme.tertiary

    AppNotificationType.CLAIM_REJECTED -> MaterialTheme.colorScheme.error
}

/**
 * Satu baris kotak masuk notifikasi: ikon beraksen, judul, isi singkat, dan
 * waktu relatif.
 *
 * Baris yang belum dibaca diberi latar lebih terang + titik aksen, sehingga
 * pengguna bisa membedakannya tanpa harus membaca ulang semuanya.
 */
@Composable
fun NotificationListItem(
    notification: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = notificationAccent(notification.type)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = notificationIcon(notification.type),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp),
                    tint = accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = RelativeTime.format(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}
