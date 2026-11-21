package org.umn.application_ketemu.ui.feature.myreports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import org.umn.application_ketemu.ui.components.LoncengNotifikasi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.components.LoadingSkeletonList
import org.umn.application_ketemu.ui.components.ReportCard

/** Durasi animasi isi & kartu halaman Aktivitas (milidetik). */
private const val CONTENT_MS = 260
private const val ITEM_MS = 280

/** Jenis keadaan layar - dipakai sebagai KUNCI animasi (bukan datanya). */
private enum class ReportsStateKind { LOADING, EMPTY, ERROR, SUCCESS }

/**
 * Daftar laporan milik pengguna yang sedang masuk.
 *
 * Animasi yang dipakai:
 *  1. Pergantian keadaan (memuat -> daftar / kosong) memakai [AnimatedContent]
 *     sehingga isi baru masuk dengan fade + naik halus, bukan berganti mendadak.
 *  2. Kartu laporan muncul BERURUTAN (stagger) agar halaman terasa hidup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    onOpenNotifications: () -> Unit = {},
    unreadNotifications: Int = 0,
    viewModel: MyReportsViewModel,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_my_reports)) },
                actions = {
                    LoncengNotifikasi(count = unreadNotifications, onClick = onOpenNotifications)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        // Kunci animasi memakai JENIS keadaan (bukan isinya) supaya daftar tidak
        // berkedip saat datanya berubah, mis. sesudah menambah laporan baru.
        val stateKind = when (uiState) {
            UiState.Loading -> ReportsStateKind.LOADING
            UiState.Empty -> ReportsStateKind.EMPTY
            is UiState.Error -> ReportsStateKind.ERROR
            is UiState.Success -> ReportsStateKind.SUCCESS
        }

        AnimatedContent(
            targetState = stateKind,
            transitionSpec = {
                (fadeIn(tween(CONTENT_MS)) +
                    slideInVertically(tween(CONTENT_MS)) { full -> full / 12 }) togetherWith
                    fadeOut(tween(CONTENT_MS / 2))
            },
            label = "myReportsContent",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { kind ->
            when (kind) {
                ReportsStateKind.LOADING -> LoadingSkeletonList(itemCount = 2)

                ReportsStateKind.EMPTY -> EmptyState(
                    title = stringResource(R.string.empty_reports_title),
                    description = stringResource(R.string.empty_reports_desc)
                )

                ReportsStateKind.ERROR -> EmptyState(
                    title = stringResource(R.string.error_generic_title),
                    description = stringResource(R.string.error_generic_desc)
                )

                ReportsStateKind.SUCCESS -> StaggeredReportList(
                    reports = (uiState as? UiState.Success)?.data.orEmpty(),
                    onReportClick = onReportClick
                )
            }
        }
    }
}

/** Daftar laporan dengan animasi masuk berurutan (stagger). */
@Composable
private fun StaggeredReportList(
    reports: List<Report>,
    onReportClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items = reports, key = { _, report -> report.id }) { index, report ->
            var visible by remember { mutableStateOf(false) }
            val delayMillis = (index % 8) * 55
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(ITEM_MS, delayMillis = delayMillis)) +
                    slideInVertically(tween(ITEM_MS, delayMillis = delayMillis)) { it / 4 }
            ) {
                ReportCard(
                    report = report,
                    onClick = { onReportClick(report.id) }
                )
            }
        }
    }
}
