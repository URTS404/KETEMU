package org.umn.application_ketemu.ui.feature.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.repository.InMemoryStaffKeyRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.UserRole

/**
 * Menguji penebusan kunci staf (jalur B).
 *
 * Yang diperiksa: kunci sah menaikkan peran sesi menjadi STAFF, kunci yang
 * sudah terpakai ditolak, dan kode asal-asalan ditolak tanpa mengubah peran.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StaffKeyViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private data class Fixture(
        val viewModel: StaffKeyViewModel,
        val keys: InMemoryStaffKeyRepository,
        val session: SessionStore
    )

    private fun fixture(): Fixture {
        val keys = InMemoryStaffKeyRepository()
        val session = SessionStore()
        session.signIn(DummyData.student)
        return Fixture(StaffKeyViewModel(keys, session), keys, session)
    }

    @Test
    fun `kunci sah menaikkan peran menjadi STAFF`() = runTest {
        val fixture = fixture()
        val code = fixture.keys.observeKeys().value.first().code

        fixture.viewModel.redeem(code)

        assertTrue(fixture.viewModel.uiState.value.isSuccess)
        assertEquals(UserRole.STAFF, fixture.session.currentUser.value.role)
    }

    @Test
    fun `kunci yang sama tidak dapat dipakai dua kali`() = runTest {
        val first = fixture()
        val code = first.keys.observeKeys().value.first().code
        first.viewModel.redeem(code)

        // Pengguna lain mencoba kunci yang sudah hangus.
        val second = Fixture(
            StaffKeyViewModel(first.keys, SessionStore().apply { signIn(DummyData.staff.copy(role = UserRole.USER)) }),
            first.keys,
            SessionStore()
        )
        second.viewModel.redeem(code)

        assertFalse(second.viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `kode salah ditolak tanpa mengubah peran`() = runTest {
        val fixture = fixture()

        fixture.viewModel.redeem("SALAH-9999")

        assertFalse(fixture.viewModel.uiState.value.isSuccess)
        assertEquals(UserRole.USER, fixture.session.currentUser.value.role)
    }

    @Test
    fun `kode terlalu pendek ditolak sebelum memanggil server`() = runTest {
        val fixture = fixture()

        fixture.viewModel.redeem("ab")

        assertFalse(fixture.viewModel.uiState.value.isSuccess)
        assertEquals("Kode kunci terlalu pendek.", fixture.viewModel.uiState.value.message)
    }
}
