package org.umn.application_ketemu.ui.feature.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.repository.InMemoryAuditLogRepository
import org.umn.application_ketemu.data.repository.InMemoryReportRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.UserRole

@OptIn(ExperimentalCoroutinesApi::class)
class AdminModerationViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private data class Fixture(
        val viewModel: AdminModerationViewModel,
        val reports: InMemoryReportRepository,
        val audit: InMemoryAuditLogRepository
    )

    private fun fixtureAs(role: UserRole): Fixture {
        val reports = InMemoryReportRepository(DummyData.reports())
        val audit = InMemoryAuditLogRepository()
        val session = SessionStore()
        val user = when (role) {
            UserRole.USER -> DummyData.student
            UserRole.STAFF -> DummyData.staff
            UserRole.ADMIN -> DummyData.staff.copy(role = UserRole.ADMIN)
        }
        session.signIn(user)
        return Fixture(AdminModerationViewModel(reports, audit, session), reports, audit)
    }

    @Test
    fun `verifikasi memindahkan status laporan dan mencatat audit`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        fixture.viewModel.verifyAndForward("rpt-006")

        assertEquals(
            ReportStatus.DISERAHKAN_KE_POS,
            fixture.reports.getReport("rpt-006")?.status
        )
        assertEquals(1, fixture.audit.observeEntries().first().size)
    }

    @Test
    fun `arsip menyembunyikan laporan dari beranda tetapi tetap tersimpan`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        fixture.viewModel.archive("rpt-001")

        assertTrue(fixture.reports.observeReports().first().none { it.id == "rpt-001" })
        assertEquals(true, fixture.reports.getReport("rpt-001")?.isArchived)
        assertEquals(1, fixture.reports.observeArchivedReports().first().size)
    }

    @Test
    fun `peran user tidak boleh memverifikasi`() = runTest {
        val fixture = fixtureAs(UserRole.USER)

        fixture.viewModel.verifyAndForward("rpt-006")

        assertEquals(ReportStatus.DILAPORKAN, fixture.reports.getReport("rpt-006")?.status)
        assertTrue(fixture.audit.observeEntries().first().isEmpty())
        assertEquals(AdminUiText.ACCESS_DENIED, fixture.viewModel.message.value)
    }
}
