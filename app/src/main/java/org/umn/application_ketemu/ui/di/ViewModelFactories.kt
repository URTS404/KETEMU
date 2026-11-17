package org.umn.application_ketemu.ui.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.umn.application_ketemu.data.AppContainer
import org.umn.application_ketemu.ui.MainViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminAuditViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminClaimsViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminDashboardViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminModerationViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminKeysViewModel
import org.umn.application_ketemu.ui.feature.profile.StaffKeyViewModel
import org.umn.application_ketemu.ui.feature.admin.AdminUsersViewModel
import org.umn.application_ketemu.ui.feature.auth.ForgotPasswordViewModel
import org.umn.application_ketemu.ui.feature.auth.LoginViewModel
import org.umn.application_ketemu.ui.feature.auth.RegisterViewModel
import org.umn.application_ketemu.ui.feature.create.CreateReportViewModel
import org.umn.application_ketemu.ui.feature.detail.ReportDetailViewModel
import org.umn.application_ketemu.ui.feature.home.HomeViewModel
import org.umn.application_ketemu.ui.feature.message.ConversationViewModel
import org.umn.application_ketemu.ui.feature.message.MessagesViewModel
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
                session = container.session,
                conversationRepository = container.conversationRepository
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

fun messagesViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            MessagesViewModel(conversationRepository = container.conversationRepository)
        }
    }

fun conversationViewModelFactory(
    container: AppContainer,
    threadId: String
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        ConversationViewModel(
            conversationRepository = container.conversationRepository,
            threadId = threadId
        )
    }
}

/** Panel admin: dashboard ringkasan. */
fun adminDashboardViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            AdminDashboardViewModel(
                reportRepository = container.reportRepository,
                claimRepository = container.claimRepository,
                auditLogRepository = container.auditLogRepository,
                session = container.session
            )
        }
    }

/** Panel admin: antrean moderasi laporan. */
fun adminModerationViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            AdminModerationViewModel(
                reportRepository = container.reportRepository,
                auditLogRepository = container.auditLogRepository,
                session = container.session
            )
        }
    }

/** Panel admin: keputusan klaim. */
fun adminClaimsViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            AdminClaimsViewModel(
                claimRepository = container.claimRepository,
                reportRepository = container.reportRepository,
                auditLogRepository = container.auditLogRepository,
                session = container.session
            )
        }
    }

/** Panel admin: log aktivitas. */
fun adminAuditViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            AdminAuditViewModel(
                auditLogRepository = container.auditLogRepository,
                session = container.session
            )
        }
    }

/** Panel admin: daftar & manajemen akun (aksi tulis hanya untuk ADMIN). */
fun adminUsersViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            AdminUsersViewModel(
                userRepository = container.userRepository,
                staffKeyRepository = container.staffKeyRepository,
                session = container.session
            )
        }
    }

/** Panel admin: daftar kunci staf (undangan sekali pakai). */
fun adminKeysViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            AdminKeysViewModel(
                staffKeyRepository = container.staffKeyRepository,
                session = container.session
            )
        }
    }

/** Layar Akun: penebusan kunci staf oleh pengguna biasa. */
fun staffKeyViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            StaffKeyViewModel(
                staffKeyRepository = container.staffKeyRepository,
                session = container.session
            )
        }
    }
