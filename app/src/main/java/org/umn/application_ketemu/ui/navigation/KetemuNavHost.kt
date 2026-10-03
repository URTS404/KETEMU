package org.umn.application_ketemu.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.umn.application_ketemu.R
import org.umn.application_ketemu.ui.LocalAppContainer
import org.umn.application_ketemu.ui.MainViewModel
import org.umn.application_ketemu.ui.components.OfflineBanner
import org.umn.application_ketemu.ui.di.createReportViewModelFactory
import org.umn.application_ketemu.ui.di.homeViewModelFactory
import org.umn.application_ketemu.ui.di.mainViewModelFactory
import org.umn.application_ketemu.ui.di.myReportsViewModelFactory
import org.umn.application_ketemu.ui.di.reportDetailViewModelFactory
import org.umn.application_ketemu.ui.feature.create.CreateReportScreen
import org.umn.application_ketemu.ui.feature.create.CreateReportViewModel
import org.umn.application_ketemu.ui.feature.detail.ReportDetailScreen
import org.umn.application_ketemu.ui.feature.detail.ReportDetailViewModel
import org.umn.application_ketemu.ui.feature.home.HomeScreen
import org.umn.application_ketemu.ui.feature.home.HomeViewModel
import org.umn.application_ketemu.ui.feature.message.MessageScreen
import org.umn.application_ketemu.ui.feature.myreports.MyReportsScreen
import org.umn.application_ketemu.ui.feature.myreports.MyReportsViewModel
import org.umn.application_ketemu.ui.feature.profile.ProfileScreen

/**
 * Kerangka utama aplikasi.
 *
 * Tiga keputusan penting di sini:
 *  1. Banner offline dipasang SEKALI di atas NavHost sehingga seluruh layar
 *     otomatis mematuhi indikator status offline yang diwajibkan PRD.
 *  2. Bottom Navigation hanya tampil pada halaman tingkat atas (Beranda,
 *     Laporan Saya, Profil) mengikuti pola Material 3.
 *  3. ViewModel dibuat lewat factory di ui/di/ViewModelFactories.kt, sehingga
 *     layer tampilan tidak pernah menyentuh repository secara langsung.
 */
@Composable
fun KetemuNavHost(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val mainViewModel: MainViewModel = viewModel(
        factory = mainViewModelFactory(container)
    )

    val isOnline by mainViewModel.isOnline.collectAsStateWithLifecycle()
    val pendingCount by mainViewModel.pendingCount.collectAsStateWithLifecycle()
    val currentUser by mainViewModel.currentUser.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = listOf(
        KetemuRoutes.HOME,
        KetemuRoutes.MY_REPORTS,
        KetemuRoutes.MESSAGE,
        KetemuRoutes.PROFILE
    )

    Column(modifier = modifier.fillMaxSize()) {
        OfflineBanner(isOnline = isOnline, pendingCount = pendingCount)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentRoute in topLevelRoutes) {
                    KetemuBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> navController.navigateToTopLevel(route) },
                        onScanClick = { navController.navigate(KetemuRoutes.CREATE_REPORT) }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = KetemuRoutes.HOME,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(KetemuRoutes.HOME) {
                    val viewModel: HomeViewModel = viewModel(
                        factory = homeViewModelFactory(container)
                    )
                    HomeScreen(
                        viewModel = viewModel,
                        onReportClick = { reportId ->
                            navController.navigate(KetemuRoutes.reportDetail(reportId))
                        },
                        onCreateReportClick = {
                            navController.navigate(KetemuRoutes.CREATE_REPORT)
                        }
                    )
                }

                composable(KetemuRoutes.MY_REPORTS) {
                    val viewModel: MyReportsViewModel = viewModel(
                        factory = myReportsViewModelFactory(container)
                    )
                    MyReportsScreen(
                        viewModel = viewModel,
                        onReportClick = { reportId ->
                            navController.navigate(KetemuRoutes.reportDetail(reportId))
                        }
                    )
                }

                composable(KetemuRoutes.MESSAGE) {
                    MessageScreen()
                }

                composable(KetemuRoutes.PROFILE) {
                    ProfileScreen(
                        user = currentUser,
                        onSwitchRole = { role -> container.session.switchRole(role) },
                        onSignOut = {
                            container.session.signOut()
                            scope.launch { container.authRepository.signOut() }
                        }
                    )
                }

                composable(KetemuRoutes.CREATE_REPORT) {
                    val viewModel: CreateReportViewModel = viewModel(
                        factory = createReportViewModelFactory(container)
                    )
                    CreateReportScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }

                composable(
                    route = KetemuRoutes.REPORT_DETAIL,
                    arguments = listOf(
                        navArgument(KetemuRoutes.ARG_REPORT_ID) {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val reportId = entry.arguments
                        ?.getString(KetemuRoutes.ARG_REPORT_ID)
                        .orEmpty()

                    val viewModel: ReportDetailViewModel = viewModel(
                        factory = reportDetailViewModelFactory(container, reportId)
                    )
                    ReportDetailScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun KetemuBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onScanClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                selected = currentRoute == KetemuRoutes.HOME,
                onClick = { onNavigate(KetemuRoutes.HOME) },
                icon = { tint -> Icon(Icons.Default.Home, contentDescription = null, tint = tint) },
                label = stringResource(R.string.nav_home),
                modifier = Modifier.weight(1f)
            )
            BottomBarItem(
                selected = currentRoute == KetemuRoutes.MY_REPORTS,
                onClick = { onNavigate(KetemuRoutes.MY_REPORTS) },
                icon = { tint -> Icon(Icons.Default.List, contentDescription = null, tint = tint) },
                label = stringResource(R.string.nav_my_reports),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FilledIconButton(
                    onClick = onScanClick,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_camera),
                        contentDescription = stringResource(R.string.nav_scan),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            BottomBarItem(
                selected = currentRoute == KetemuRoutes.MESSAGE,
                onClick = { onNavigate(KetemuRoutes.MESSAGE) },
                icon = { tint ->
                    Icon(painterResource(R.drawable.ic_chat), contentDescription = null, tint = tint)
                },
                label = stringResource(R.string.nav_message),
                modifier = Modifier.weight(1f)
            )
            BottomBarItem(
                selected = currentRoute == KetemuRoutes.PROFILE,
                onClick = { onNavigate(KetemuRoutes.PROFILE) },
                icon = { tint -> Icon(Icons.Default.Person, contentDescription = null, tint = tint) },
                label = stringResource(R.string.nav_profile),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            icon(tint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1
        )
    }
}

/**
 * Perpindahan antar menu utama tidak menumpuk di back stack, sehingga pengguna
 * tidak perlu menekan tombol kembali berkali-kali.
 */
private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
