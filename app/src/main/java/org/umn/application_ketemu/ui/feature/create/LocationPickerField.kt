package org.umn.application_ketemu.ui.feature.create

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.location.LocationProvider
import org.umn.application_ketemu.domain.model.GeoPoint
import org.umn.application_ketemu.ui.components.KetemuMap

/**
 * Penanda titik lokasi barang di peta, pelengkap kolom teks lokasi.
 *
 * Dua cara mengisi titik:
 *  1. Ketuk peta (tidak butuh izin maupun GPS) — berguna di dalam gedung.
 *  2. Tekan "Gunakan lokasi saya" — izin lokasi diminta saat itu juga.
 *
 * Bila GPS/izin gagal, komponen hanya menampilkan pesan; formulir dan kolom
 * teks lokasi tetap dapat diselesaikan sehingga laporan tidak pernah buntu.
 */
@Composable
fun LocationPickerField(
    point: GeoPoint?,
    onPointChange: (GeoPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationProvider = remember(context) { LocationProvider(context) }
    val scope = rememberCoroutineScope()

    var messageRes by remember { mutableStateOf<Int?>(null) }
    var isLocating by remember { mutableStateOf(false) }

    fun fetchLocation() {
        scope.launch {
            isLocating = true
            val location = locationProvider.currentLocation()
            isLocating = false
            if (location != null && location.isValid) {
                messageRes = null
                onPointChange(location)
            } else {
                messageRes = R.string.create_location_unavailable
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { granted -> granted }) {
            fetchLocation()
        } else {
            messageRes = R.string.create_location_permission_denied
        }
    }

    fun locateUser() {
        messageRes = null
        if (!locationProvider.isLocationEnabled()) {
            messageRes = R.string.create_location_disabled
            return
        }
        val granted = LocationProvider.LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            fetchLocation()
        } else {
            permissionLauncher.launch(LocationProvider.LOCATION_PERMISSIONS)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.create_location_map_label),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = stringResource(R.string.create_location_map_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            KetemuMap(
                marker = point,
                modifier = Modifier.fillMaxSize(),
                interactive = true,
                onMarkerChange = { selected ->
                    messageRes = null
                    onPointChange(selected)
                }
            )
        }

        Text(
            text = stringResource(R.string.map_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { locateUser() },
                enabled = !isLocating
            ) {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.create_location_gps_loading))
                } else {
                    Text(stringResource(R.string.create_location_gps_cta))
                }
            }

            if (point != null) {
                TextButton(
                    onClick = {
                        messageRes = null
                        onPointChange(null)
                    }
                ) {
                    Text(stringResource(R.string.create_location_clear))
                }
            }
        }

        point?.let { selected ->
            Text(
                text = stringResource(
                    R.string.create_location_coordinates,
                    selected.toCoordinateText()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        messageRes?.let { res ->
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
