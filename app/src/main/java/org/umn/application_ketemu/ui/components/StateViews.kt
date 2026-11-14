package org.umn.application_ketemu.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.umn.application_ketemu.R

/**
 * Layar/keadaan kosong.
 *
 * PRD melarang tampilan yang membingungkan; karena itu keadaan kosong selalu
 * menjelaskan penyebabnya dan menawarkan satu aksi lanjutan.
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StateIcon(icon = icon, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

/** Keadaan gagal memuat data (mis. koneksi terputus) dengan tombol coba lagi. */
@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    message: String? = null,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StateIcon(
                icon = Icons.Default.Warning,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.error_generic_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message ?: stringResource(R.string.error_generic_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onRetry) {
                Text(text = stringResource(R.string.action_retry))
            }
        }
    }
}

/**
 * Lingkaran lembut berisi ikon, dipakai bersama oleh EmptyState & ErrorState
 * agar keadaan kosong/error tidak tampak seperti layar yang rusak.
 */
@Composable
private fun StateIcon(icon: ImageVector, tint: Color) {
    Surface(
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}


/**
 * Kerangka pemuatan berbentuk kartu (skeleton).
 *
 * Dipakai sebagai pengganti spinner agar perpindahan layar terasa lebih tenang
 * dan tata letak tidak melompat ketika data masuk.
 */
@Composable
fun LoadingSkeletonList(
    modifier: Modifier = Modifier,
    itemCount: Int = 3
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .shimmer()
            )
        }
    }
}

/**
 * Efek "shimmer": kilau lembut bergerak melintasi permukaan.
 *
 * Dipakai kartu kerangka (skeleton) agar proses memuat terasa hidup, bukan
 * sekadar kotak abu yang statis.
 */
@Composable
private fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    val base = MaterialTheme.colorScheme.surfaceContainer
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest

    return drawWithCache {
        val band = size.width * 0.6f
        val startX = -band + progress * (size.width + 2 * band)
        val brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(startX, 0f),
            end = Offset(startX + band, size.height)
        )
        onDrawBehind { drawRect(brush) }
    }
}
