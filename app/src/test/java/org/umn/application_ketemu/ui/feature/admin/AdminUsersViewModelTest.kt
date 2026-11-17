package org.umn.application_ketemu.ui.feature.admin

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
import org.umn.application_ketemu.data.repository.InMemoryUserRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.UserRole

/**
 * Menguji pemisahan hak STAFF vs ADMIN pada layar Pengguna.
 *
 * Yang dijaga di sini adalah lapisan aplikasi; penegakan servernya ada pada
 * policy `profiles_admin_all` di `db/0011_admin_management.sql`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminUsersViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private data class Fixture(
        val viewModel: AdminUsersViewModel,
        val users: InMemoryUserRepository
    )

    private fun fixtureAs(role: UserRole): Fixture {
        val users = InMemoryUserRepository(listOf(DummyData.student, DummyData.staff))
        val session = SessionStore()
        session.signIn(
            when (role) {
                UserRole.USER -> DummyData.student
                UserRole.STAFF -> DummyData.staff
                UserRole.ADMIN -> DummyData.staff.copy(role = UserRole.ADMIN)
            }
        )
        return Fixture(
            AdminUsersViewModel(users, InMemoryStaffKeyRepository(), session),
            users
        )
    }

    @Test
    fun `staf hanya bisa melihat dan tidak mendapat aksi tulis`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        assertFalse(fixture.viewModel.uiState.value.canManage)
        assertEquals(2, fixture.viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `staf tidak dapat mengubah peran meski aksi dipanggil langsung`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        fixture.viewModel.changeRole(DummyData.student.id, UserRole.ADMIN)

        val student = fixture.users.observeUsers().value
            .first { it.id == DummyData.student.id }
        assertEquals(UserRole.USER, student.role)
        assertEquals(AdminUiText.ACCESS_DENIED, fixture.viewModel.uiState.value.message)
    }

    @Test
    fun `admin dapat mengubah peran non-staf pada akun lain`() = runTest {
        val fixture = fixtureAs(UserRole.ADMIN)

        assertTrue(fixture.viewModel.uiState.value.canManage)
        fixture.viewModel.changeRole(DummyData.student.id, UserRole.ADMIN)

        val student = fixture.users.observeUsers().value
            .first { it.id == DummyData.student.id }
        assertEquals(UserRole.ADMIN, student.role)
    }

    @Test
    fun `kenaikan menjadi STAFF wajib memakai kunci`() = runTest {
        val fixture = fixtureAs(UserRole.ADMIN)

        fixture.viewModel.changeRole(DummyData.student.id, UserRole.STAFF)

        val student = fixture.users.observeUsers().value
            .first { it.id == DummyData.student.id }
        assertEquals(UserRole.USER, student.role)
        assertEquals(
            "Pakai kunci staf untuk mengangkat akun menjadi STAFF.",
            fixture.viewModel.uiState.value.message
        )
    }

    @Test
    fun `admin dapat memblokir akun lain`() = runTest {
        val fixture = fixtureAs(UserRole.ADMIN)

        fixture.viewModel.toggleBanned(DummyData.student.id)

        val student = fixture.users.observeUsers().value
            .first { it.id == DummyData.student.id }
        assertTrue(student.isBanned)
    }

    @Test
    fun `admin tidak dapat memblokir akun sendiri`() = runTest {
        val fixture = fixtureAs(UserRole.ADMIN)
        val selfId = DummyData.staff.id

        fixture.viewModel.toggleBanned(selfId)

        val self = fixture.users.observeUsers().value.first { it.id == selfId }
        assertFalse(self.isBanned)
        assertEquals(
            "Anda tidak dapat memblokir akun sendiri.",
            fixture.viewModel.uiState.value.message
        )
    }

    @Test
    fun `verifikasi dapat dicabut dan dipulihkan oleh admin`() = runTest {
        val fixture = fixtureAs(UserRole.ADMIN)
        val id = DummyData.student.id
        // DummyData.student sudah terverifikasi, jadi aksi pertama mencabut.
        val before = fixture.users.observeUsers().value.first { it.id == id }.isVerified

        fixture.viewModel.toggleVerified(id)
        assertEquals(
            !before,
            fixture.users.observeUsers().value.first { it.id == id }.isVerified
        )

        fixture.viewModel.toggleVerified(id)
        assertEquals(
            before,
            fixture.users.observeUsers().value.first { it.id == id }.isVerified
        )
    }
}
