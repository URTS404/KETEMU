package org.umn.application_ketemu.ui.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.umn.application_ketemu.data.AppContainer
import org.umn.application_ketemu.ui.MainViewModel
import org.umn.application_ketemu.ui.feature.auth.ForgotPasswordViewModel
import org.umn.application_ketemu.ui.feature.auth.LoginViewModel
import org.umn.application_ketemu.ui.feature.auth.RegisterViewModel
import org.umn.application_ketemu.ui.feature.create.CreateReportViewModel
import org.umn.application_ketemu.ui.feature.detail.ReportDetailViewModel
import org.umn.application_ketemu.ui.feature.home.HomeViewModel
import org.umn.application_ketemu.ui.feature.myreports.MyReportsViewModel

/**
 * Kumpulan factory ViewModel (penyediaan dependensi manual).
 *
 * Dikumpulkan di satu berkas agar alur dependensi mudah diaudit penguji:
 * ViewModel -> Repository -> sumber data.
 *
 * Saat Hilt dipasang (M0-Backend), berkas ini digantikan oleh @Module +
 * @HiltViewModel, dan setiap ViewModel menerima dependensinya lewat constructor
 * injection — pemanggilan di layer navigasi tetap sama bentuknya.
 */
fun mainViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            MainViewModel(
                networkMonitor = container.networkMonitor,
                reportRepository = container.reportRepository,
                session = container.session
            )
        }
    }

fun loginViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            LoginViewModel(
                session = container.session,
                authRepository = container.authRepository
            )
        }
    }

fun registerViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            RegisterViewModel(
                session = container.session,
                authRepository = container.authRepository
            )
        }
    }

fun forgotPasswordViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            ForgotPasswordViewModel(authRepository = container.authRepository)
        }
    }

fun homeViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            HomeViewModel(
                reportRepository = container.reportRepository,
                networkMonitor = container.networkMonitor,
                session = container.session
            )
        }
    }

fun myReportsViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            MyReportsViewModel(
                reportRepository = container.reportRepository,
                session = container.session
            )
        }
    }

fun createReportViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            CreateReportViewModel(
                reportRepository = container.reportRepository,
                networkMonitor = container.networkMonitor,
                session = container.session
            )
        }
    }

fun reportDetailViewModelFactory(
    container: AppContainer,
    reportId: String
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        ReportDetailViewModel(
            reportRepository = container.reportRepository,
            reportId = reportId
        )
    }
}
