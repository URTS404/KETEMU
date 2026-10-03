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
 * Pengujian fitur Pendaftaran akun (Register).
 *
 * Domain dipasok eksplisit agar pengujian tidak bergantung isi local.properties.
 * Jaringan digantikan [FakeAuthRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

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
    ) = RegisterViewModel(
        session = session,
        authRepository = repository,
        campusDomain = campusDomain
    )

    @Test
    fun pendaftaranValidMembukaSesi() {
        val session = SessionStore()
        val viewModel = buildViewModel(session)

        // Email diberi spasi + huruf besar: harus dinormalkan.
        viewModel.onFullNameChange("Siti Aminah")
        viewModel.onEmailChange("  Siti.Aminah@UMN.ac.id ")
        viewModel.onNimChange("00000067890")
        viewModel.onPasswordChange("rahasia123")
        viewModel.onConfirmPasswordChange("rahasia123")
        viewModel.onRegister()

        assertNull(viewModel.uiState.value.error)
        assertTrue(session.isLoggedIn.value)
        assertEquals("siti.aminah@umn.ac.id", session.currentUser.value.email)
        assertEquals("Siti Aminah", session.currentUser.value.fullName)
    }

    @Test
    fun kataSandiTidakCocokDitolak() {
        val session = SessionStore()
        val viewModel = buildViewModel(session)

        viewModel.onFullNameChange("Siti Aminah")
        viewModel.onEmailChange("siti.aminah@umn.ac.id")
        viewModel.onNimChange("00000067890")
        viewModel.onPasswordChange("rahasia123")
        viewModel.onConfirmPasswordChange("rahasia999")
        viewModel.onRegister()

        assertEquals(RegisterError.PASSWORD_MISMATCH, viewModel.uiState.value.error)
        assertFalse(session.isLoggedIn.value)
    }

    @Test
    fun emailSudahTerdaftarDitolak() {
        val session = SessionStore()
        val repository = FakeAuthRepository().apply {
            addUser("budi.santoso@umn.ac.id", "rahasia123")
        }
        val viewModel = buildViewModel(session, repository)

        viewModel.onFullNameChange("Budi Santoso")
        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        viewModel.onNimChange("00000012345")
        viewModel.onPasswordChange("rahasia123")
        viewModel.onConfirmPasswordChange("rahasia123")
        viewModel.onRegister()

        assertEquals(RegisterError.EMAIL_TAKEN, viewModel.uiState.value.error)
        assertFalse(session.isLoggedIn.value)
    }

    @Test
    fun gangguanJaringanDitolak() {
        val session = SessionStore()
        val repository = FakeAuthRepository().apply { networkError = true }
        val viewModel = buildViewModel(session, repository)

        viewModel.onFullNameChange("Siti Aminah")
        viewModel.onEmailChange("siti.aminah@umn.ac.id")
        viewModel.onNimChange("00000067890")
        viewModel.onPasswordChange("rahasia123")
        viewModel.onConfirmPasswordChange("rahasia123")
        viewModel.onRegister()

        assertEquals(RegisterError.NETWORK, viewModel.uiState.value.error)
        assertFalse(session.isLoggedIn.value)
    }
}
