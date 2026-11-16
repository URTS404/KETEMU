package org.umn.application_ketemu.ui.feature.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.domain.model.GeoPoint
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.domain.model.ReportCategory

/** Isi formulir yang dikirim pengguna saat menekan "Simpan Laporan". */
data class ReportDraft(
    val type: ItemType,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val location: String,
    /** Titik peta opsional; kolom teks [location] tetap wajib dan bisa diubah. */
    val coordinates: GeoPoint? = null,
    val photoUri: String?
)

/**
 * Formulir laporan barang hilang / ditemukan.
 *
 * Form tetap berfungsi penuh saat offline — inti janji produk pada PRD.
 * Pesan setelah menyimpan membedakan dua kondisi:
 *  - offline -> "Tersimpan di HP. Akan dikirim otomatis saat online."
 *  - online  -> "Laporan berhasil dikirim."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    viewModel: CreateReportViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val saveResult by viewModel.lastSaveResult.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val offlineMessage = stringResource(R.string.create_saved_offline)
    val onlineMessage = stringResource(R.string.create_saved_online)

    LaunchedEffect(saveResult) {
        val result = saveResult ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = if (result.savedOffline) offlineMessage else onlineMessage
        )
        viewModel.consumeSaveResult()
        onSaved()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        ReportForm(
            isOnline = isOnline,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onSubmit = { draft ->
                viewModel.saveReport(
                    type = draft.type,
                    title = draft.title,
                    description = draft.description,
                    category = draft.category,
                    location = draft.location,
                    coordinates = draft.coordinates,
                    photoUri = draft.photoUri
                )
            }
        )
    }
}

/**
 * Isi formulir. State input disimpan dengan rememberSaveable sehingga terpulihkan
 * saat rotasi layar maupun proses sistem mematikan aplikasi sementara — penting
 * karena pengguna bisa saja sedang kehilangan sinyal ketika mengisi laporan.
 */
@Composable
private fun ReportForm(
    isOnline: Boolean,
    onSubmit: (ReportDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    var type by rememberSaveable { mutableStateOf(ItemType.LOST) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ReportCategory.LAINNYA) }
    var location by rememberSaveable { mutableStateOf("") }
    // Titik peta disimpan sebagai dua Double (bukan objek GeoPoint) agar tetap
    // bertahan saat rotasi layar lewat rememberSaveable.
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }

    val coordinates = latitude?.let { lat ->
        longitude?.let { lng -> GeoPoint(latitude = lat, longitude = lng) }
    }

    // Baru di-set true setelah pengguna menekan "Simpan Laporan"; memicu
    // tampilnya pesan error per-field (isError + supportingText).
    var showErrors by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isOnline) {
            Text(
                text = stringResource(R.string.create_offline_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Text(
            text = stringResource(R.string.create_type_label),
            style = MaterialTheme.typography.labelLarge
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == ItemType.LOST,
                onClick = { type = ItemType.LOST },
                label = { Text(stringResource(R.string.filter_lost)) }
            )
            FilterChip(
                selected = type == ItemType.FOUND,
                onClick = { type = ItemType.FOUND },
                label = { Text(stringResource(R.string.filter_found)) }
            )
        }

        PhotoPickerField(
            photoUri = photoUri,
            onPhotoTaken = { photoUri = it }
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            label = { Text(stringResource(R.string.create_field_title)) },
            placeholder = { Text(stringResource(R.string.create_field_title_hint)) },
            isError = showErrors && title.isBlank(),
            supportingText = if (showErrors && title.isBlank()) {
                { Text(stringResource(R.string.create_error_title)) }
            } else {
                null
            }
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = MaterialTheme.shapes.small,
            label = { Text(stringResource(R.string.create_field_description)) },
            placeholder = {
                Text(stringResource(R.string.create_field_description_hint))
            }
        )

        CategorySelector(selected = category, onSelect = { category = it })

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            label = { Text(stringResource(R.string.create_field_location)) },
            placeholder = { Text(stringResource(R.string.create_field_location_hint)) },
            isError = showErrors && location.isBlank(),
            supportingText = if (showErrors && location.isBlank()) {
                { Text(stringResource(R.string.create_error_location)) }
            } else {
                null
            }
        )

        LocationPickerField(
            point = coordinates,
            onPointChange = { point ->
                latitude = point?.latitude
                longitude = point?.longitude
                // Kolom teks tetap bisa diedit bebas. Isi otomatis hanya bila
                // masih kosong supaya deskripsi yang sudah ditulis tidak hilang.
                if (point != null && location.isBlank()) {
                    location = point.toCoordinateText()
                }
            }
        )

        Button(
            onClick = {
                // Submission diblokir sampai field wajib valid; bila belum,
                // tampilkan pesan error alih-alih mengirim laporan.
                if (title.isBlank() || location.isBlank()) {
                    showErrors = true
                } else {
                    onSubmit(
                        ReportDraft(
                            type = type,
                            title = title,
                            description = description,
                            category = category,
                            location = location,
                            coordinates = coordinates,
                            photoUri = photoUri
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text(
                text = stringResource(R.string.create_save),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
