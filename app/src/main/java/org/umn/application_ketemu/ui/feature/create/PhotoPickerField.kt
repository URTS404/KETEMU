package org.umn.application_ketemu.ui.feature.create

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import org.umn.application_ketemu.R
import java.io.File

/**
 * Pemilih foto barang memakai kamera perangkat.
 *
 * Acceptance Criteria PRD #3 melarang force crash saat mengambil gambar, jadi:
 *  1. Izin CAMERA diminta saat dibutuhkan, bukan saat aplikasi dibuka.
 *  2. Bila izin ditolak/kamera tak tersedia, pengguna diberi tahu dan laporan
 *     tetap bisa disimpan tanpa foto (tidak ada jalan buntu).
 *  3. Foto ditulis ke cache internal lalu dibagikan lewat FileProvider.
 */
@Composable
fun PhotoPickerField(
    photoUri: String?,
    onPhotoTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val deniedMessage = stringResource(R.string.create_camera_denied)
    val unavailableMessage = stringResource(R.string.create_camera_unavailable)
    val shape = RoundedCornerShape(16.dp)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingFile
        if (success && file != null && file.exists()) {
            onPhotoTaken(Uri.fromFile(file).toString())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            message = deniedMessage
        } else {
            pendingFile = createPhotoFile(context)
            pendingFile?.let { file -> cameraLauncher.launch(file.toShareableUri(context)) }
        }
    }

    fun takePhoto() {
        message = null
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        pendingFile = createPhotoFile(context)
        val file = pendingFile
        if (file == null) {
            message = unavailableMessage
        } else {
            cameraLauncher.launch(file.toShareableUri(context))
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (photoUri == null) {
                Text(
                    text = stringResource(R.string.create_photo_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                AsyncImage(
                    model = photoUri,
                    contentDescription = stringResource(R.string.create_photo_cta),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                )
            }
        }

        OutlinedButton(
            onClick = { takePhoto() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    if (photoUri == null) R.string.create_photo_cta
                    else R.string.create_photo_retake
                )
            )
        }

        message?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Membuat berkas tujuan di cache/photos/ (lihat res/xml/file_paths.xml). */
private fun createPhotoFile(context: Context): File? {
    val directory = File(context.cacheDir, "photos")
    if (!directory.exists() && !directory.mkdirs()) return null
    return File(directory, "ketemu_${System.currentTimeMillis()}.jpg")
}

/** Menghasilkan content URI yang aman dibagikan ke aplikasi kamera. */
private fun File.toShareableUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)
