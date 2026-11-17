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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.repository.InMemoryAuditLogRepository
import org.umn.application_ketemu.data.repository.InMemoryClaimRepository
import org.umn.application_ketemu.data.repository.InMemoryNotificationRepository
import org.umn.application_ketemu.data.repository.InMemoryReportRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.AppNotificationType
import org.umn.application_ketemu.domain.model.ClaimStatus
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.UserRole

@OptIn(ExperimentalCoroutinesApi::class)
class AdminClaimsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private data class Fixture(
        val viewModel: AdminClaimsViewModel,
        val claims: InMemoryClaimRepository,
        val reports: InMemoryReportRepository,
        val audit: InMemoryAuditLogRepository,
        val notifications: InMemoryNotificationRepository
    )

    private fun fixtureAs(role: UserRole): Fixture {
        val claims = InMemoryClaimRepository(DummyData.claims())
        val reports = InMemoryReportRepository(DummyData.reports())
        val audit = InMemoryAuditLogRepository()
        val notifications = InMemoryNotificationRepository()
        val session = SessionStore()
        session.signIn(
            when (role) {
                UserRole.USER -> DummyData.student
                UserRole.STAFF -> DummyData.staff
                UserRole.ADMIN -> DummyData.staff.copy(role = UserRole.ADMIN)
            }
        )
        return Fixture(
            AdminClaimsViewModel(claims, reports, audit, notifications, session),
            claims,
            reports,
            audit,
            notifications
        )
    }

    @Test
    fun `setujui klaim memberi kode dan memindahkan laporan ke siap diambil`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        fixture.viewModel.approve("clm-001")

        val claim = fixture.claims.getClaim("clm-001")
        assertEquals(ClaimStatus.DISETUJUI, claim?.status)
        assertNotNull(claim?.pickupCode)
        assertEquals(ReportStatus.SIAP_DIAMBIL, fixture.reports.getReport("rpt-002")?.status)
        assertEquals(1, fixture.audit.observeEntries().first().size)
        assertEquals("KTM atas nama Andi Wijaya", fixture.viewModel.lastDecision.value?.claimTitle)

        val approval = fixture.notifications.observeNotifications().first()
            .single { it.type == AppNotificationType.CLAIM_APPROVED }
        assertEquals("user-lain-03", approval.recipientId)
        assertEquals("rpt-002", approval.reportId)
        assertTrue(approval.message.contains(claim?.pickupCode!!))
        assertTrue(!approval.isRead)
    }

    @Test
    fun `tolak klaim tidak mengubah status laporan`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        fixture.viewModel.reject("clm-002", note = "Ciri barang tidak cocok")

        assertEquals(ClaimStatus.DITOLAK, fixture.claims.getClaim("clm-002")?.status)
        assertEquals(
            ReportStatus.DISERAHKAN_KE_POS,
            fixture.reports.getReport("rpt-005")?.status
        )

        val rejection = fixture.notifications.observeNotifications().first()
            .single { it.type == AppNotificationType.CLAIM_REJECTED }
        assertEquals("user-lain-06", rejection.recipientId)
        assertTrue(rejection.message.contains("Ciri barang tidak cocok"))
    }

    @Test
    fun `peran user tidak boleh memutuskan klaim`() = runTest {
        val fixture = fixtureAs(UserRole.USER)

        fixture.viewModel.approve("clm-001")

        assertEquals(ClaimStatus.MENUNGGU, fixture.claims.getClaim("clm-001")?.status)
        assertTrue(fixture.audit.observeEntries().first().isEmpty())
        assertTrue(fixture.notifications.observeNotifications().first().isEmpty())
        assertEquals(AdminUiText.ACCESS_DENIED, fixture.viewModel.message.value)
    }
}
