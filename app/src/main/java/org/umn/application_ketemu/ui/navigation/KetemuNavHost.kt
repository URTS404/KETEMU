package org.umn.application_ketemu.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
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
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.ui.LocalAppContainer
import org.umn.application_ketemu.ui.MainViewModel
import org.umn.application_ketemu.ui.components.OfflineBanner
import org.umn.application_ketemu.ui.di.adminAuditViewModelFactory
import org.umn.application_ketemu.ui.di.adminClaimsViewModelFactory
import org.umn.application_ketemu.ui.di.adminDashboardViewModelFactory
import org.umn.application_ketemu.ui.di.adminKeysViewModelFactory
import org.umn.application_ketemu.ui.di.adminModerationViewModelFactory
import org.umn.application_ketemu.ui.di.staffKeyViewModelFactory
import org.umn.application_ketemu.ui.di.adminUsersViewModelFactory
import org.umn.application_ketemu.ui.di.conversationViewModelFactory
import org.umn.application_ketemu.ui.di.createReportViewModelFactory
import org.umn.application_ketemu.ui.di.homeViewModelFactory
import org.umn.application_ketemu.ui.di.mainViewModelFactory
import org.umn.application_ketemu.ui.di.messagesViewModelFactory
import org.umn.application_ketemu.ui.di.myReportsViewModelFactory
import org.umn.application_ketemu.ui.di.reportDetailViewModelFactory
import org.umn.application_ketemu.ui.feature.admin.AdminAuditScreen
import org.umn.application_ketemu.ui.feature.admin.AdminAuditViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminClaimsScreen
import org.umn.application_ketemu.ui.feature.admin.AdminClaimsViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminDashboardScreen
import org.umn.application_ketemu.ui.feature.admin.AdminDashboardViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminKeysScreen
import org.umn.application_ketemu.ui.feature.admin.AdminKeysViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminModerationScreen
import org.umn.application_ketemu.ui.feature.admin.AdminModerationViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminUsersScreen
import org.umn.application_ketemu.ui.feature.admin.AdminUsersViewModel
import org.umn.application_ketemu.ui.feature.create.CreateReportScreen
import org.umn.application_ketemu.ui.feature.create.CreateReportViewModel
import org.umn.application_ketemu.ui.feature.detail.ReportDetailScreen
import org.umn.application_ketemu.ui.feature.detail.ReportDetailViewModel
import org.umn.application_ketemu.ui.feature.home.HomeScreen
import org.umn.application_ketemu.ui.feature.home.HomeViewModel
import org.umn.application_ketemu.ui.feature.message.ConversationScreen
import org.umn.application_ketemu.ui.feature.message.ConversationViewModel
import org.umn.application_ketemu.ui.feature.message.MessageScreen
import org.umn.application_ketemu.ui.feature.message.MessagesViewModel
import org.umn.application_ketemu.ui.feature.myreports.MyReportsScreen
import org.umn.application_ketemu.ui.feature.myreports.MyReportsViewModel
import org.umn.application_ketemu.ui.feature.profile.ProfileScreen
import org.umn.application_ketemu.ui.feature.profile.StaffKeyScreen
import org.umn.application_ketemu.ui.feature.profile.StaffKeyViewModel

/*
 * ============================================================================
 *  Transisi navigasi
 * ----------------------------------------------------------------------------
 *  Durasi sengaja singkat (~300ms) agar pindah layar terasa responsif.
 *  Tiga pola:
 *   - Tab (Beranda/Aktivitas/Pesan/Akun) : fade cepat, tanpa geser.
 *   - Layar tumpukan (List -> Detail)    : geser dari kanan (pola "push").
 *   - Formulir (Buat Laporan)            : naik dari bawah seperti lembaran.
 * ============================================================================
 */
private const val NAV_ANIM_MS = 300

private val navFadeIn: EnterTransition = fadeIn(tween(NAV_ANIM_MS))
private val navFadeOut: ExitTransition = fadeOut(tween(NAV_ANIM_MS))

/** Push: layar baru masuk dari kanan, layar lama bergeser sedikit ke kiri. */
private val pushEnter: EnterTransition =
    slideInHorizontally(tween(NAV_ANIM_MS), initialOffsetX = { full -> full }) +
        fadeIn(tween(NAV_ANIM_MS))
private val pushExit: ExitTransition =
    slideOutHorizontally(tween(NAV_ANIM_MS), targetOffsetX = { full -> -full / 3 }) +
        fadeOut(tween(NAV_ANIM_MS))
private val pushPopEnter: EnterTransition =
    slideInHorizontally(tween(NAV_ANIM_MS), initialOffsetX = { full -> -full / 3 }) +
        fadeIn(tween(NAV_ANIM_MS))
private val pushPopExit: ExitTransition =
    slideOutHorizontally(tween(NAV_ANIM_MS), targetOffsetX = { full -> full }) +
        fadeOut(tween(NAV_ANIM_MS))

/** Sheet: formulir naik dari bawah, lalu turun lagi saat ditutup. */
private val sheetEnter: EnterTransition =
    slideInVertically(tween(NAV_ANIM_MS), initialOffsetY = { full -> full }) +
        fadeIn(tween(NAV_ANIM_MS))
private val sheetPopExit: ExitTransition =
    slideOutVertically(tween(NAV_ANIM_MS), targetOffsetY = { full -> full }) +
        fadeOut(tween(NAV_ANIM_MS))

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
                modifier = Modifier.padding(innerPadding),
                // Bawaan: fade cepat untuk pergantian tab antar layar tingkat atas.
                enterTransition = { navFadeIn },
                exitTransition = { navFadeOut },
                popEnterTransition = { navFadeIn },
                popExitTransition = { navFadeOut }
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
                        },
                        onOpenMessages = {
                            navController.navigateToTopLevel(KetemuRoutes.MESSAGE)
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
                    val viewModel: MessagesViewModel = viewModel(
                        factory = messagesViewModelFactory(container)
                    )
                    MessageScreen(
                        viewModel = viewModel,
                        onThreadClick = { threadId ->
                            navController.navigate(KetemuRoutes.conversation(threadId))
                        }
                    )
                }

                composable(KetemuRoutes.PROFILE) {
                    ProfileScreen(
                        user = currentUser,
                        onSwitchRole = { role -> container.session.switchRole(role) },
                        onOpenAdmin = { navController.navigate(KetemuRoutes.ADMIN) },
                        onOpenStaffKey = { navController.navigate(KetemuRoutes.STAFF_KEY) },
                        onSignOut = {
                            container.session.signOut()
                            scope.launch { container.authRepository.signOut() }
                        }
                    )
                }

                composable(
                    route = KetemuRoutes.CREATE_REPORT,
                    // Terasa seperti lembaran yang naik dari bawah.
                    enterTransition = { sheetEnter },
                    exitTransition = { navFadeOut },
                    popEnterTransition = { navFadeIn },
                    popExitTransition = { sheetPopExit }
                ) {
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
                    ),
                    // Pola "push": geser dari kanan saat membuka detail.
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
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

                composable(
                    route = KetemuRoutes.CONVERSATION,
                    arguments = listOf(
                        navArgument(KetemuRoutes.ARG_THREAD_ID) {
                            type = NavType.StringType
                        }
                    ),
                    // Pola "push": geser dari kanan saat membuka percakapan.
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
                ) { entry ->
                    val threadId = entry.arguments
                        ?.getString(KetemuRoutes.ARG_THREAD_ID)
                        .orEmpty()

                    val viewModel: ConversationViewModel = viewModel(
                        factory = conversationViewModelFactory(container, threadId)
                    )
                    ConversationScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ================= Panel admin (STAFF/ADMIN) =================
                composable(
                    route = KetemuRoutes.ADMIN,
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
                ) {
                    val viewModel: AdminDashboardViewModel = viewModel(
                        factory = adminDashboardViewModelFactory(container)
                    )
                    AdminDashboardScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenModeration = { navController.navigate(KetemuRoutes.ADMIN_MODERATION) },
                        onOpenClaims = { navController.navigate(KetemuRoutes.ADMIN_CLAIMS) },
                        onOpenAudit = { navController.navigate(KetemuRoutes.ADMIN_AUDIT) },
                        onOpenUsers = { navController.navigate(KetemuRoutes.ADMIN_USERS) },
                        onOpenKeys = { navController.navigate(KetemuRoutes.ADMIN_KEYS) }
                    )
                }

                composable(
                    route = KetemuRoutes.ADMIN_MODERATION,
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
                ) {
                    val viewModel: AdminModerationViewModel = viewModel(
                        factory = adminModerationViewModelFactory(container)
                    )
                    AdminModerationScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = KetemuRoutes.ADMIN_CLAIMS,
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
                ) {
                    val viewModel: AdminClaimsViewModel = viewModel(
                        factory = adminClaimsViewModelFactory(container)
                    )
                    AdminClaimsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = KetemuRoutes.ADMIN_AUDIT,
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
                ) {
                    val viewModel: AdminAuditViewModel = viewModel(
                        factory = adminAuditViewModelFactory(container)
                    )
                    AdminAuditScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = KetemuRoutes.ADMIN_USERS,
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
                ) {
                    val viewModel: AdminUsersViewModel = viewModel(
                        factory = adminUsersViewModelFactory(container)
                    )
                    AdminUsersScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = KetemuRoutes.ADMIN_KEYS,
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
                ) {
                    val viewModel: AdminKeysViewModel = viewModel(
                        factory = adminKeysViewModelFactory(container)
                    )
                    AdminKeysScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Jalur B: pengguna biasa menebus kunci staf yang diberikan admin.
                composable(
                    route = KetemuRoutes.STAFF_KEY,
                    enterTransition = { pushEnter },
                    exitTransition = { pushExit },
                    popEnterTransition = { pushPopEnter },
                    popExitTransition = { pushPopExit }
                ) {
                    val viewModel: StaffKeyViewModel = viewModel(
                        factory = staffKeyViewModelFactory(container)
                    )
                    StaffKeyScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/** Tinggi bar navigasi, tinggi tonjolan, dan ukuran tombol scan ("ngambang"). */
private val BAR_HEIGHT = 72.dp
private val SCAN_OVERHANG = 22.dp
private val SCAN_BUTTON_SIZE = 58.dp

@Composable
private fun KetemuBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onScanClick: () -> Unit
) {
    // Tinggi slot = tinggi bar + ruang tonjolan tombol scan.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT + SCAN_OVERHANG)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BAR_HEIGHT)
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
                    icon = { tint ->
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = tint
                        )
                    },
                    label = stringResource(R.string.nav_my_reports),
                    modifier = Modifier.weight(1f)
                )
                // Ruang kosong di tengah agar tombol scan tidak menutupi tab lain.
                Spacer(modifier = Modifier.weight(1f))
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

        // Tombol scan diletakkan DI LUAR Surface bar: Surface meng-clip kontennya,
        // jadi kalau berada di dalam tidak akan bisa menonjol ke atas.
        FloatingActionButton(
            onClick = onScanClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(SCAN_BUTTON_SIZE),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = stringResource(R.string.nav_scan),
                modifier = Modifier.size(28.dp)
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
    // Warna ikon & "pill" dianimasikan halus saat tab berpindah.
    val tint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tabTint"
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        label = "tabPill"
    )

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 30.dp)
                .clip(CircleShape)
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                icon(tint)
            }
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
