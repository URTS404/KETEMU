package org.umn.application_ketemu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.Report

/**
 * Kartu laporan bergaya "foto dulu": gambar di atas, kotak deskripsi di bawah.
 *
 * @param compact dipakai saat kartu tampil di grid 2 kolom: gambar lebih kotak
 *   (4:3), tipografi & padding lebih rapat, serta status memakai chip yang bisa
 *   membungkus (FlowRow) agar tetap rapi pada lebar sempit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportPhotoCard(
    report: Report,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    // Jarak dalam kartu dibuat sama untuk kedua mode (lebih lega / minimalis).
    val contentPadding = 14.dp
    val blockSpacing = 10.dp
    val descPadding = 12.dp
    val location = report.pos?.name ?: report.lastSeenLocation
    val time = RelativeTime.format(report.createdAt)

    // Mikro-animasi: kartu sedikit mengecil saat ditekan -> terasa taktil.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "reportCardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            ReportPhoto(report = report, compact = compact)

            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(blockSpacing)
            ) {
                Text(
                    text = report.title,
                    style = if (compact) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Kotak deskripsi: permukaan berbeda agar terbaca sebagai blok.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = report.description.ifBlank {
                            stringResource(R.string.report_description_empty)
                        },
                        modifier = Modifier.padding(descPadding),
                        style = if (compact) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 3 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (compact) {
                    // Grid sempit: lokasi + waktu disatukan, chip status dibungkus.
                    MetaLine(text = "$location · $time")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReportStatusChip(status = report.status)
                        SyncStatusChip(syncStatus = report.syncStatus)
                    }
                } else {
                    MetaLine(text = location)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusChipRow(status = report.status, syncStatus = report.syncStatus)
                    }
                }
            }
        }
    }
}

/** Baris kecil: ikon lokasi + satu baris teks yang dipangkas bila kepanjangan. */
@Composable
private fun MetaLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Area gambar + badge jenis barang di sudut kiri atas. */
@Composable
private fun ReportPhoto(report: Report, compact: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (compact) 4f / 3f else 16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (report.photoUri.isNullOrBlank()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 28.dp else 34.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.report_photo_empty),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            AsyncImage(
                model = report.photoUri,
                contentDescription = report.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        TypeBadge(
            type = report.type,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(if (compact) 8.dp else 10.dp)
        )
    }
}

/** Label kecil "Hilang"/"Ditemukan" sebagai penanda jenis barang. */
@Composable
private fun TypeBadge(type: ItemType, modifier: Modifier = Modifier) {
    val isLost = type == ItemType.LOST
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = if (isLost) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary
        }
    ) {
        Text(
            text = stringResource(
                if (isLost) R.string.filter_lost else R.string.filter_found
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isLost) {
                MaterialTheme.colorScheme.onTertiary
            } else {
                MaterialTheme.colorScheme.onPrimary
            }
        )
    }
}