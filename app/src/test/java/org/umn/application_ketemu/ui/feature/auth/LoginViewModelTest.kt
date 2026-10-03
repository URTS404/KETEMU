package org.umn.application_ketemu.ui.feature.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.umn.application_ketemu.data.session.SessionStore

/**
 * Pengujian aturan masuk (login) — inti Acceptance Criteria #1 PRD
 * ("hanya pengguna kampus yang boleh masuk").
 *
 * Dijalankan tanpa emulator: `gradlew :app:testDebugUnitTest`. Autentikasi
 * jaringan digantikan [FakeAuthRepository] agar cepat dan bisa dijalankan di CI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val campusDomain = "umn.ac.id"

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        session: SessionStore = SessionStore(),
        repository: FakeAuthRepository = FakeAuthRepository()
    ) = LoginViewModel(
        session = session,
        authRepository = repository,
        campusDomain = campusDomain
    )

    @Test
    fun emailKosongDitolakDanSesiTetapTertutup() {
        val session = SessionStore()
        val viewModel = buildViewModel(session)

        viewModel.onPasswordChange("rahasia123")
        viewModel.onSignIn()

        assertEquals(LoginError.EMAIL_EMPTY, viewModel.uiState.value.error)
        assertFalse(session.isLoggedIn.value)
    }

    @Test
    fun kataSandiKurangDariEnamKarakterDitolak() {
        val session = SessionStore()
        val viewModel = buildViewModel(session)

        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        viewModel.onPasswordChange("12345")
        viewModel.onSignIn()

        assertEquals(LoginError.PASSWORD_TOO_SHORT, viewModel.uiState.value.error)
        assertFalse(session.isLoggedIn.value)
    }

    @Test
    fun emailNonKampusDitolakMeskiKataSandiValid() {
        val session = SessionStore()
        val viewModel = buildViewModel(session)

        viewModel.onEmailChange("budi.santoso@gmail.com")
        viewModel.onPasswordChange("rahasia123")
        viewModel.onSignIn()

        assertEquals(LoginError.NOT_CAMPUS_EMAIL, viewModel.uiState.value.error)
        assertFalse(session.isLoggedIn.value)
    }

    @Test
    fun kredensialTerdaftarMembukaSesi() {
        val session = SessionStore()
        val repository = FakeAuthRepository().apply {
            addUser("budi.santoso@umn.ac.id", "rahasia123")
        }
        val viewModel = buildViewModel(session, repository)

        // Email diberi spasi + huruf besar: harus dinormalkan sebelum dikirim.
        viewModel.onEmailChange("  Budi.Santoso@UMN.ac.id ")
        viewModel.onPasswordChange("rahasia123")
        viewModel.onSignIn()

        assertNull(viewModel.uiState.value.error)
        assertTrue(session.isLoggedIn.value)
        assertEquals("budi.santoso@umn.ac.id", session.currentUser.value.email)
    }

    @Test
    fun kredensialSalahDitolak() {
        val session = SessionStore()
        val repository = FakeAuthRepository().apply {
            addUser("budi.santoso@umn.ac.id", "rahasia123")
        }
        val viewModel = buildViewModel(session, repository)

        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        viewModel.onPasswordChange("salah123")
        viewModel.onSignIn()

        assertEquals(LoginError.INVALID_CREDENTIALS, viewModel.uiState.value.error)
        assertFalse(session.isLoggedIn.value)
    }

    @Test
    fun gangguanJaringanDitolak() {
        val session = SessionStore()
        val repository = FakeAuthRepository().apply { networkError = true }
        val viewModel = buildViewModel(session, repository)

        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        viewModel.onPasswordChange("rahasia123")
        viewModel.onSignIn()

        assertEquals(LoginError.NETWORK, viewModel.uiState.value.error)
        assertFalse(session.isLoggedIn.value)
    }

    @Test
    fun mengetikUlangMenghapusPesanKesalahanSebelumnya() {
        val viewModel = buildViewModel()

        viewModel.onSignIn()
        assertEquals(LoginError.EMAIL_EMPTY, viewModel.uiState.value.error)

        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun tombolMasukNonaktifSelamaIsianBelumLengkap() {
        val viewModel = buildViewModel()

        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPasswordChange("rahasia123")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun keluarMenutupKembaliSesi() {
        val session = SessionStore()
        val repository = FakeAuthRepository().apply {
            addUser("budi.santoso@umn.ac.id", "rahasia123")
        }
        val viewModel = buildViewModel(session, repository)

        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        viewModel.onPasswordChange("rahasia123")
        viewModel.onSignIn()
        assertTrue(session.isLoggedIn.value)

        session.signOut()
        assertFalse(session.isLoggedIn.value)
    }
}
