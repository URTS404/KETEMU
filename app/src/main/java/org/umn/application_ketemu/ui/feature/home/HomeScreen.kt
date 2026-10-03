package org.umn.application_ketemu.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.domain.model.ItemType
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.components.LoadingSkeletonList
import org.umn.application_ketemu.ui.components.ReportCard

/**
 * Layar Beranda (Home Feed).
 *
 * Elemen wajib PRD ada di sini: filter Hilang/Ditemukan, pencarian, daftar
 * berbentuk Card, dan tombol Call-to-Action berwarna Orange.
 *
 * Catatan MVVM: state input (kata kunci & filter) disimpan di layar agar bisa
 * dipulihkan saat rotasi (rememberSaveable), lalu diteruskan ke ViewModel yang
 * melakukan penyaringan data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onReportClick: (String) -> Unit,
    onCreateReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf<ItemType?>(null) }

    LaunchedEffect(query) { viewModel.onSearchQueryChange(query) }
    LaunchedEffect(selectedType) { viewModel.onTypeFilterChange(selectedType) }

    val isFiltering = query.isNotBlank() || selectedType != null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.action_report_lost)) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = onCreateReportClick,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchAndFilterBar(
                query = query,
                onQueryChange = { query = it },
                selectedType = selectedType,
                onTypeChange = { selectedType = it }
            )

            when (val state = uiState) {
                UiState.Loading -> LoadingSkeletonList()

                UiState.Empty -> EmptyState(
                    title = stringResource(
                        if (isFiltering) R.string.empty_search_title
                        else R.string.empty_reports_title
                    ),
                    description = stringResource(
                        if (isFiltering) R.string.empty_search_desc
                        else R.string.empty_reports_desc
                    ),
                    actionLabel = if (isFiltering) null else stringResource(
                        R.string.action_report_lost
                    ),
                    onAction = if (isFiltering) null else onCreateReportClick
                )

                is UiState.Error -> EmptyState(
                    title = stringResource(R.string.error_generic_title),
                    description = stringResource(R.string.error_generic_desc)
                )

                is UiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = state.data, key = { it.id }) { report ->
                        ReportCard(
                            report = report,
                            onClick = { onReportClick(report.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedType: ItemType?,
    onTypeChange: (ItemType?) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text(stringResource(R.string.home_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeChange(null) },
                label = { Text(stringResource(R.string.filter_all)) }
            )
            FilterChip(
                selected = selectedType == ItemType.LOST,
                onClick = { onTypeChange(ItemType.LOST) },
                label = { Text(stringResource(R.string.filter_lost)) }
            )
            FilterChip(
                selected = selectedType == ItemType.FOUND,
                onClick = { onTypeChange(ItemType.FOUND) },
                label = { Text(stringResource(R.string.filter_found)) }
            )
        }
    }
}
