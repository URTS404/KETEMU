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
import org.junit.Assert.assertNull
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
import org.umn.application_ketemu.domain.model.AuditAction
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.ClaimStatus
import org.umn.application_ketemu.domain.model.HandoverResult
import org.umn.application_ketemu.domain.model.ReportStatus
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.ClaimRepository

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

    private fun fixtureAs(role: UserRole): Fixture = fixtureAs(role, DummyData.claims())

    private fun fixtureAs(role: UserRole, initialClaims: List<Claim>): Fixture {
        val claims = InMemoryClaimRepository(initialClaims)
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
    fun `setujui klaim botol menerbitkan kode untuk pengklaim botol`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        fixture.viewModel.approve("clm-004")

        val claim = fixture.claims.getClaim("clm-004")
        assertEquals(ClaimStatus.DISETUJUI, claim?.status)
        assertNotNull(claim?.pickupCode)
        assertEquals(ReportStatus.SIAP_DIAMBIL, fixture.reports.getReport("rpt-007")?.status)

        val approval = fixture.notifications.observeNotifications().first()
            .single { it.type == AppNotificationType.CLAIM_APPROVED }
        assertEquals(DummyData.student.id, approval.recipientId)
        assertEquals("rpt-007", approval.reportId)
        assertTrue(approval.message.contains(claim?.pickupCode!!))
    }

    @Test
    fun `antrean klaim membawa detail barang dan pos untuk dialog tinjauan`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        val details = fixture.viewModel.reportsById.first { it.isNotEmpty() }
        val bottle = DummyData.reports().first { it.id == "rpt-007" }

        assertEquals(bottle.pos?.name, details["rpt-007"]?.pos?.name)

        val queued = fixture.viewModel.pendingClaims.first { it.isNotEmpty() }
        assertTrue(queued.any { it.id == "clm-004" })
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

    /** Klaim yang sudah disetujui dan barangnya siap diambil di pos. */
    private fun klaimSiapDiambil(id: String, reportId: String, kode: String) = Claim(
        id = id,
        reportId = reportId,
        reportTitle = "KTM atas nama Andi Wijaya",
        claimantId = "user-lain-03",
        claimantName = "Andi Wijaya",
        claimantEmail = "andi.wijaya@student.umn.ac.id",
        answer = "NIM 00000077777",
        status = ClaimStatus.DISETUJUI,
        createdAt = 1_700_000_000_000,
        decidedAt = 1_700_000_100_000,
        decidedBy = "Pak Rahmat",
        pickupCode = kode,
        reportStatus = ReportStatus.SIAP_DIAMBIL
    )

    @Test
    fun `serah terima menutup laporan temuan dan mencatat audit sekali`() = runTest {
        val klaim = klaimSiapDiambil("clm-777", "rpt-002", "ABC123")
        val fixture = fixtureAs(UserRole.STAFF, listOf(klaim))

        assertEquals(1, fixture.viewModel.readyForPickup.first { it.isNotEmpty() }.size)

        fixture.viewModel.completeHandover(klaim, "abc123")

        assertNotNull(fixture.claims.getClaim("clm-777")?.handedOverAt)
        assertEquals(ReportStatus.SELESAI_DIKLAIM, fixture.reports.getReport("rpt-002")?.status)
        assertEquals(AdminUiText.HANDOVER_DONE, fixture.viewModel.message.value)

        // Demo tidak punya fungsi database, jadi aplikasi mencatat auditnya sendiri.
        val audit = fixture.audit.observeEntries().first()
        assertEquals(1, audit.size)
        assertEquals(AuditAction.SERAH_TERIMA_BARANG, audit.single().action)
        assertEquals("CLAIM", audit.single().targetType)
        assertEquals("clm-777", audit.single().targetId)

        assertTrue(fixture.viewModel.readyForPickup.value.isEmpty())
    }

    @Test
    fun `serah terima dengan kode salah tidak mengubah laporan`() = runTest {
        val klaim = klaimSiapDiambil("clm-778", "rpt-002", "ABC123")
        val fixture = fixtureAs(UserRole.STAFF, listOf(klaim))
        val statusAwal = fixture.reports.getReport("rpt-002")?.status

        fixture.viewModel.completeHandover(klaim, "SALAH1")

        assertEquals(AdminUiText.HANDOVER_FAILED, fixture.viewModel.message.value)
        assertNull(fixture.claims.getClaim("clm-778")?.handedOverAt)
        assertEquals(statusAwal, fixture.reports.getReport("rpt-002")?.status)
        assertTrue(fixture.audit.observeEntries().first().isEmpty())
    }

    @Test
    fun `audit tidak dicatat aplikasi bila fungsi database sudah mencatat`() = runTest {
        val klaim = klaimSiapDiambil("clm-779", "rpt-002", "ABC123")
        val dasar = InMemoryClaimRepository(listOf(klaim))
        // Meniru jalur RPC db/0028, yang sudah menulis audit_logs sendiri.
        val serverClaims = object : ClaimRepository by dasar {
            override suspend fun completeHandover(claimId: String, code: String?) =
                HandoverResult(berhasil = true, auditDiServer = true)
        }

        val reports = InMemoryReportRepository(DummyData.reports())
        val audit = InMemoryAuditLogRepository()
        val session = SessionStore().apply { signIn(DummyData.staff) }
        val viewModel = AdminClaimsViewModel(
            serverClaims,
            reports,
            audit,
            InMemoryNotificationRepository(),
            session
        )

        viewModel.completeHandover(klaim, "ABC123")

        assertEquals(AdminUiText.HANDOVER_DONE, viewModel.message.value)
        assertTrue(audit.observeEntries().first().isEmpty())
    }
}
