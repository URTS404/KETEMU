package org.umn.application_ketemu.ui.feature.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.umn.application_ketemu.R
import org.umn.application_ketemu.domain.model.ReportCategory

/** Pemilih kategori barang (Elektronik / Dokumen / Aksesoris / Lainnya). */
@Composable
fun CategorySelector(
    selected: ReportCategory,
    onSelect: (ReportCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.create_field_category),
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportCategory.entries.forEach { category ->
                FilterChip(
                    selected = selected == category,
                    onClick = { onSelect(category) },
                    label = { Text(category.displayName()) }
                )
            }
        }
    }
}

private fun ReportCategory.displayName(): String = when (this) {
    ReportCategory.ELEKTRONIK -> "Elektronik"
    ReportCategory.DOKUMEN -> "Dokumen"
    ReportCategory.AKSESORIS -> "Aksesoris"
    ReportCategory.LAINNYA -> "Lainnya"
}
