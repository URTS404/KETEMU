package org.umn.application_ketemu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/** Tiga fase umpan balik pengiriman yang ditampilkan sebagai dialog besar. */
enum class SubmissionStatus {
    SAVING,
    SUCCESS,
    ERROR
}

/**
 * Dialog umpan balik untuk aksi kirim/simpan yang butuh waktu.
 *
 * Snackbar mudah terlewat karena tampil kecil dan singkat. Dialog ini memakai
 * seluruh perhatian pengguna dengan tiga animasi berbeda:
 *  - [SubmissionStatus.SAVING]: cincin berputar (sedang diproses),
 *  - [SubmissionStatus.SUCCESS]: centang yang tergambar + memantul,
 *  - [SubmissionStatus.ERROR]: ikon peringatan yang bergetar.
 *
 * Selama proses, dialog tidak bisa ditutup agar pengguna tidak mengira
 * pengiriman sudah selesai. Status berhasil/gagal tetap menyediakan tombol
 * eksplisit, jadi pengguna punya waktu membaca hasilnya.
 */
@Composable
fun SubmissionFeedbackDialog(
    status: SubmissionStatus,
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = status != SubmissionStatus.SAVING,
            // Ketukan tak sengaja di luar dialog tidak boleh menghilangkan
            // informasi hasil yang baru saja muncul.
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 6.dp,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StatusArtwork(status = status)

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        minLines = 3,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    FeedbackActions(
                        status = status,
                        primaryActionLabel = primaryActionLabel,
                        onPrimaryAction = onPrimaryAction,
                        secondaryActionLabel = secondaryActionLabel,
                        onSecondaryAction = onSecondaryAction
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackActions(
    status: SubmissionStatus,
    primaryActionLabel: String?,
    onPrimaryAction: (() -> Unit)?,
    secondaryActionLabel: String?,
    onSecondaryAction: (() -> Unit)?
) {
    val showPrimary = status != SubmissionStatus.SAVING &&
        primaryActionLabel != null &&
        onPrimaryAction != null
    val showSecondary = status == SubmissionStatus.ERROR &&
        secondaryActionLabel != null &&
        onSecondaryAction != null

    /*
     * Area aksi selalu memesan tinggi yang sama, termasuk ketika status masih
     * SAVING. Tanpa ini kartu dialog bertambah tinggi saat tombol muncul dan
     * — karena dialog berada di tengah layar — seluruh isinya terlihat naik.
     * Tombol kedua diletakkan sebaris agar tinggi tidak bertambah lagi.
     */
    Spacer(modifier = Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showPrimary,
            enter = fadeIn(tween(durationMillis = 180, delayMillis = 90)),
            exit = fadeOut(tween(durationMillis = 100))
        ) {
            if (showSecondary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onSecondaryAction.invoke() },
                        modifier = Modifier
                            .weight(0.8f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = secondaryActionLabel.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = { onPrimaryAction?.invoke() },
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = primaryActionLabel.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onPrimaryAction?.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        text = primaryActionLabel.orEmpty(),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Ilustrasi status. Latar lingkaran, ikon, dan gerakannya berbeda per fase
 * supaya pengguna bisa memahami hasil hanya dari warna + gerak, tanpa harus
 * membaca teks lebih dulu.
 */
@Composable
private fun StatusArtwork(status: SubmissionStatus) {
    val containerColor by animateColorAsState(
        targetValue = when (status) {
            SubmissionStatus.SAVING,
            SubmissionStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
            SubmissionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = tween(durationMillis = 260),
        label = "feedbackContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            SubmissionStatus.SAVING,
            SubmissionStatus.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
            SubmissionStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        },
        animationSpec = tween(durationMillis = 260),
        label = "feedbackContentColor"
    )

    /*
     * Hanya isi lingkaran yang dianimasikan, bukan lingkarannya. Sebelumnya
     * seluruh ilustrasi di-reset ke skala 0,72 setiap ganti status sehingga
     * terlihat berkedip. Centang juga digambar bersamaan dengan scale-in,
     * bukan menunggu scale-in selesai.
     */
    val contentScale = remember { Animatable(0.92f) }
    val shake = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(status) {
        shake.snapTo(0f)
        checkProgress.snapTo(0f)
        contentScale.snapTo(0.92f)

        launch {
            contentScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        when (status) {
            SubmissionStatus.SUCCESS -> checkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 430,
                    easing = FastOutSlowInEasing
                )
            )

            SubmissionStatus.ERROR -> shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 460
                    0f at 0
                    -14f at 70
                    14f at 140
                    -10f at 210
                    10f at 280
                    -5f at 350
                    0f at 460
                }
            )

            SubmissionStatus.SAVING -> Unit
        }
    }

    Box(
        modifier = Modifier.size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = containerColor)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                    translationX = shake.value
                },
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                SubmissionStatus.SAVING -> {
                    SavingRing(color = contentColor)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                SubmissionStatus.SUCCESS -> {
                    SuccessCheck(
                        progress = checkProgress.value,
                        color = contentColor
                    )
                }

                SubmissionStatus.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}


/** Busur berputar sebagai penanda proses yang sedang berjalan. */
@Composable
private fun SavingRing(color: Color) {
    val transition = rememberInfiniteTransition(label = "savingFeedback")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 980, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "savingFeedbackRotation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 110f,
            useCenter = false,
            style = Stroke(
                width = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * Centang digambar bertahap memakai [PathMeasure] agar muncul seperti ditulis
 * dengan pena, bukan sekadar ikon yang tiba-tiba tampil.
 */
@Composable
private fun SuccessCheck(
    progress: Float,
    color: Color
) {
    Canvas(modifier = Modifier.size(58.dp)) {
        if (progress <= 0f) return@Canvas

        val checkPath = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.52f)
            lineTo(size.width * 0.40f, size.height * 0.76f)
            lineTo(size.width * 0.87f, size.height * 0.24f)
        }
        val measure = PathMeasure().apply {
            setPath(checkPath, false)
        }
        val visiblePath = Path()
        measure.getSegment(
            startDistance = 0f,
            stopDistance = measure.length * progress,
            destination = visiblePath,
            startWithMoveTo = true
        )

        drawPath(
            path = visiblePath,
            color = color,
            style = Stroke(
                width = 6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

