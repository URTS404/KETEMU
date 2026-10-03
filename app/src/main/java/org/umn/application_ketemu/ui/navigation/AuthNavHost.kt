package org.umn.application_ketemu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.umn.application_ketemu.ui.LocalAppContainer
import org.umn.application_ketemu.ui.di.forgotPasswordViewModelFactory
import org.umn.application_ketemu.ui.di.loginViewModelFactory
import org.umn.application_ketemu.ui.di.registerViewModelFactory
import org.umn.application_ketemu.ui.feature.auth.ForgotPasswordScreen
import org.umn.application_ketemu.ui.feature.auth.LoginScreen
import org.umn.application_ketemu.ui.feature.auth.RegisterScreen

/**
 * Rute navigasi khusus alur autentikasi (sebelum pengguna masuk).
 *
 * Dipisah dari [KetemuRoutes] karena alur ini ditutup begitu sesi terbuka —
 * gerbang aplikasi berpindah ke [KetemuNavHost].
 */
object AuthRoutes {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val FORGOT_PASSWORD = "auth/forgot_password"
}

/**
 * NavHost alur autentikasi: Login, Pendaftaran, dan Lupa Kata Sandi.
 *
 * Dipakai [KetemuAppGate] saat sesi belum terbuka. Tiap layar membuat ViewModel
 * lewat factory di ui/di/ViewModelFactories.kt (tidak menyentuh repository
 * secara langsung).
 */
@Composable
fun AuthNavHost(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthRoutes.LOGIN,
        modifier = modifier
    ) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                viewModel = viewModel(factory = loginViewModelFactory(container)),
                onRegisterClick = { navController.navigate(AuthRoutes.REGISTER) },
                onForgotPasswordClick = { navController.navigate(AuthRoutes.FORGOT_PASSWORD) }
            )
        }

        composable(AuthRoutes.REGISTER) {
            RegisterScreen(
                viewModel = viewModel(factory = registerViewModelFactory(container)),
                onBack = { navController.popBackStack() }
            )
        }

        composable(AuthRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = viewModel(factory = forgotPasswordViewModelFactory(container)),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
