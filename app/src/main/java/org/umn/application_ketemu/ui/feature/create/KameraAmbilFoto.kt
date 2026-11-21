package org.umn.application_ketemu.ui.feature.create

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.umn.application_ketemu.R
import java.io.File

/**
 * Layar kamera dalam aplikasi memakai CameraX (Modul 9 & 10).
 *
 * Alasan tidak lagi memakai intent kamera sistem: pratinjau jadi milik aplikasi
 * sendiri sehingga pengguna melihat bingkai foto, bisa mengulang, dan hasilnya
 * langsung tersimpan di cache internal tanpa berpindah aplikasi.
 *
 * Kegagalan kamera TIDAK mematikan alur: pesan galat ditampilkan, dan pengguna
 * tetap bisa memilih foto dari galeri lewat [PhotoPickerField].
 */
@Composable
fun KameraAmbilFoto(
    onFoto: (String) -> Unit,
    onTutup: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var sedangSimpan by remember { mutableStateOf(false) }
    var galat by remember { mutableStateOf<String?>(null) }

    val galatPesan = stringResource(R.string.camera_error)

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    fun ambilFoto() {
        val capture = imageCapture
        if (capture == null || sedangSimpan) return

        val directory = File(context.cacheDir, "photos")
        if (!directory.exists() && !directory.mkdirs()) {
            galat = galatPesan
            return
        }
        val berkas = File(directory, "ketemu_${System.currentTimeMillis()}.jpg")
        val opsi = ImageCapture.OutputFileOptions.Builder(berkas).build()

        sedangSimpan = true
        galat = null

        capture.takePicture(
            opsi,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    sedangSimpan = false
                    onFoto(Uri.fromFile(berkas).toString())
                }

                override fun onError(exception: ImageCaptureException) {
                    sedangSimpan = false
                    galat = exception.message ?: galatPesan
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onTutup,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val penyedia = ProcessCameraProvider.getInstance(ctx)
                    penyedia.addListener(
                        {
                            val kamera = runCatching { penyedia.get() }.getOrNull()
                            if (kamera == null) {
                                galat = galatPesan
                                return@addListener
                            }

                            val pratinjau = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val hasil = runCatching {
                                kamera.unbindAll()
                                kamera.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    pratinjau,
                                    capture
                                )
                            }
                            hasil.onFailure { galat = it.message ?: galatPesan }
                        },
                        ContextCompat.getMainExecutor(ctx)
                    )

                    previewView
                }
            )

            IconButton(
                onClick = onTutup,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.camera_close),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                galat?.let { pesan ->
                    Text(
                        text = pesan,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFB4AB),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    TextButton(onClick = onTutup) {
                        Text(
                            text = stringResource(R.string.camera_use_gallery),
                            color = Color.White
                        )
                    }
                }

                if (sedangSimpan) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = stringResource(R.string.camera_saving),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                } else {
                    IconButton(
                        onClick = { ambilFoto() },
                        modifier = Modifier
                            .size(76.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = stringResource(R.string.camera_capture),
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
