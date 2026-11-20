package org.umn.application_ketemu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Kartu pesan singkat (pengganti kotak hitam bawaan Snackbar).
 *
 * Tujuannya dua: pesan galat tampil sebagai kartu berwarna lembut dengan ikon
 * peringatan, dan pesan biasa tampil sebagai kartu netral. Ini supaya pengguna
 * tidak melihat "teks putih di atas latar hitam" yang terasa seperti crash.
 */
@Composable
fun PesanSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val pesan = data.visuals.message
        val gagal = pesan.isPesanGagal()
        val isi = if (gagal) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (gagal) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = isi,
            border = BorderStroke(
                1.dp,
                if (gagal) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            ),
            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (gagal) Icons.Filled.Warning else Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (gagal) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = pesan,
                    style = MaterialTheme.typography.bodyMedium,
                    color = isi
                )
            }
        }
    }
}

/** Pesan dianggap galat bila memuat kata kunci kegagalan yang umum dipakai aplikasi. */
private fun String.isPesanGagal(): Boolean {
    val teks = lowercase()
    return listOf(
        "gagal",
        "tidak bisa",
        "tidak dapat",
        "belum bisa",
        "tidak terhubung",
        "periksa koneksi",
        "kesalahan",
        "kode pengambilan tidak cocok"
    ).any { teks.contains(it) }
}
