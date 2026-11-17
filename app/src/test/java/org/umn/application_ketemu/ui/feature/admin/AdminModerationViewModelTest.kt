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
import org.umn.application_ketemu.data.repository.InMemoryNotificationRepository
import org.umn.application_ketemu.data.repository.InMemoryReportRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.AppNotificationType
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
        val audit: InMemoryAuditLogRepository,
        val notifications: InMemoryNotificationRepository
    )

    private fun fixtureAs(role: UserRole): Fixture {
        val reports = InMemoryReportRepository(DummyData.reports())
        val audit = InMemoryAuditLogRepository()
        val notifications = InMemoryNotificationRepository()
        val session = SessionStore()
        val user = when (role) {
            UserRole.USER -> DummyData.student
            UserRole.STAFF -> DummyData.staff
            UserRole.ADMIN -> DummyData.staff.copy(role = UserRole.ADMIN)
        }
        session.signIn(user)
        return Fixture(
            AdminModerationViewModel(reports, audit, notifications, session),
            reports,
            audit,
            notifications
        )
    }

    @Test
    fun `verifikasi menyimpan pos tujuan dan mencatat audit`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        // rpt-008 adalah barang temuan yang belum diverifikasi (masih DILAPORKAN).
        fixture.viewModel.verifyAndForward("rpt-008", "pos-gedung-b")

        val report = fixture.reports.getReport("rpt-008")
        assertEquals(ReportStatus.DISERAHKAN_KE_POS, report?.status)
        // Pos dipilih satpam, bukan pelapor: harus ikut tersimpan di laporan.
        assertEquals("pos-gedung-b", report?.pos?.id)
        assertEquals("Pos Gedung B", report?.pos?.name)

        val audit = fixture.audit.observeEntries().first().single()
        assertTrue(audit.note.orEmpty().contains("Pos Gedung B"))

        val notification = fixture.notifications.observeNotifications().first().single()
        assertEquals(AppNotificationType.REPORT_STATUS_CHANGED, notification.type)
        assertEquals(DummyData.student.id, notification.recipientId)
        assertEquals("rpt-008", notification.reportId)
        assertTrue(notification.message.contains("Pos Gedung B"))
        // Penerimanya penemu, bukan pemilik barang: jangan sampai pesannya
        // menyuruh membawa bukti kepemilikan seperti pada alur klaim.
        assertTrue(!notification.message.contains("bukti kepemilikan"))
        assertTrue(notification.message.contains("Beranda"))
        assertTrue(!notification.isRead)
    }

    @Test
    fun `laporan kehilangan tidak bisa diserahkan ke pos`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        // rpt-006 adalah laporan kehilangan: jalurnya Arsipkan, bukan serah-terima.
        fixture.viewModel.verifyAndForward("rpt-006", "pos-gedung-b")

        assertEquals(ReportStatus.DILAPORKAN, fixture.reports.getReport("rpt-006")?.status)
        assertEquals(null, fixture.reports.getReport("rpt-006")?.pos)
        assertTrue(fixture.audit.observeEntries().first().isEmpty())
        assertTrue(fixture.notifications.observeNotifications().first().isEmpty())
        assertEquals(AdminUiText.LOST_NOT_FORWARDABLE, fixture.viewModel.message.value)
    }

    @Test
    fun `antrean memuat laporan hilang maupun temuan yang belum diverifikasi`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        val queue = fixture.reports.observeModerationQueue().first().associateBy { it.id }

        // Laporan hilang tetap ditinjau agar bisa diarsipkan bila tidak pantas.
        assertTrue(queue.containsKey("rpt-001"))
        assertTrue(queue.containsKey("rpt-006"))
        // Barang temuan baru menunggu verifikasi sebelum tampil di Beranda.
        assertTrue(queue.containsKey("rpt-008"))
        // Laporan yang sudah diproses tidak lagi masuk antrean.
        assertTrue(!queue.containsKey("rpt-007"))
    }

    @Test
    fun `pos tidak dikenal menolak verifikasi tanpa mengubah data`() = runTest {
        val fixture = fixtureAs(UserRole.STAFF)

        fixture.viewModel.verifyAndForward("rpt-008", "pos-tidak-ada")

        assertEquals(ReportStatus.DILAPORKAN, fixture.reports.getReport("rpt-008")?.status)
        assertEquals(null, fixture.reports.getReport("rpt-008")?.pos)
        assertTrue(fixture.audit.observeEntries().first().isEmpty())
        assertTrue(fixture.notifications.observeNotifications().first().isEmpty())
        assertEquals(AdminUiText.POS_REQUIRED, fixture.viewModel.message.value)
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

        fixture.viewModel.verifyAndForward("rpt-008", "pos-gedung-c")

        assertEquals(ReportStatus.DILAPORKAN, fixture.reports.getReport("rpt-008")?.status)
        assertTrue(fixture.audit.observeEntries().first().isEmpty())
        assertTrue(fixture.notifications.observeNotifications().first().isEmpty())
        assertEquals(AdminUiText.ACCESS_DENIED, fixture.viewModel.message.value)
    }
}
