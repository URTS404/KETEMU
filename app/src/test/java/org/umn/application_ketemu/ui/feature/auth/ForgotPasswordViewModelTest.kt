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

/**
 * Pengujian fitur "Lupa Kata Sandi".
 *
 * Domain dipasok eksplisit agar pengujian tidak bergantung isi local.properties.
 * Jaringan digantikan [FakeAuthRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val campusDomain = "umn.ac.id"

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(repository: FakeAuthRepository = FakeAuthRepository()) =
        ForgotPasswordViewModel(authRepository = repository, campusDomain = campusDomain)

    @Test
    fun emailKosongDitolak() {
        val viewModel = buildViewModel()

        viewModel.onSendReset()

        assertEquals(ForgotPasswordError.EMAIL_EMPTY, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun emailNonKampusDitolak() {
        val viewModel = buildViewModel()

        viewModel.onEmailChange("budi.santoso@gmail.com")
        viewModel.onSendReset()

        assertEquals(ForgotPasswordError.NOT_CAMPUS_EMAIL, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun kirimTautanResetBerhasil() {
        val viewModel = buildViewModel()

        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        viewModel.onSendReset()

        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun gangguanJaringanDitolak() {
        val repository = FakeAuthRepository().apply { networkError = true }
        val viewModel = buildViewModel(repository)

        viewModel.onEmailChange("budi.santoso@umn.ac.id")
        viewModel.onSendReset()

        assertEquals(ForgotPasswordError.NETWORK, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSuccess)
    }
}
