package org.umn.application_ketemu.ui.feature.detail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.repository.InMemoryClaimRepository
import org.umn.application_ketemu.data.repository.InMemoryMatchRepository
import org.umn.application_ketemu.data.repository.InMemoryReportRepository
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.ClaimStatus
import org.umn.application_ketemu.domain.model.Report
import org.umn.application_ketemu.domain.model.ReportMatch
import org.umn.application_ketemu.domain.model.ReportStatus

/**
 * Uji alur klaim sisi pengguna (Tahap 4).
 *
 * Yang dijaga di sini adalah syarat yang menentukan barang boleh diklaim atau
 * tidak — bukan tampilan. Syarat itu dipakai dialog "Ini Barang Saya", jadi
 * bila salah, pengguna bisa mengklaim barang orang lain atau mengirim klaim dua
 * kali untuk botol yang sama.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Fixture(
        val viewModel: ReportDetailViewModel,
        val claims: InMemoryClaimRepository
    )

    private fun fixture(
        reportId: String,
        reports: List<Report> = DummyData.reports(),
        matches: List<ReportMatch> = DummyData.matches()
    ): Fixture {
        val claimRepository = InMemoryClaimRepository(DummyData.claims())
        val session = SessionStore().apply { signIn(DummyData.student) }
        val viewModel = ReportDetailViewModel(
            reportRepository = InMemoryReportRepository(reports),
            matchRepository = InMemoryMatchRepository(matches),
            claimRepository = claimRepository,
            session = session,
            reportId = reportId
        )
        return Fixture(viewModel, claimRepository)
    }

    /**
     * StateFlow dari `stateIn(WhileSubscribed)` baru menghitung nilai setelah
     * ada kolektor; uji ini menyalakan kolektor di latar belakang supaya
     * `canClaim.value` mencerminkan data, bukan nilai awal `false`.
     */
    private fun TestScope.startCollecting(viewModel: ReportDetailViewModel): Job =
        backgroundScope.launch(dispatcher) {
            launch { viewModel.canClaim.collect {} }
            launch { viewModel.myClaim.collect {} }
        }

    /** Salinan data contoh dengan satu barang temuan yang dilaporkan pengguna sendiri. */
    private fun reportsWithOwnFoundItem(): List<Report> {
        val found = DummyData.reports().first { it.id == "rpt-005" }
        val mine = found.copy(
            id = "rpt-009",
            ownerId = DummyData.student.id,
            ownerName = DummyData.student.fullName
        )
        return DummyData.reports() + mine
    }

    @Test
    fun `barang temuan yang sudah di pos bisa diklaim`() = runTest {
        val fixture = fixture("rpt-005")
        startCollecting(fixture.viewModel)
        runCurrent()

        assertTrue(fixture.viewModel.canClaim.value)
        assertNull(fixture.viewModel.myClaim.value)
    }

    @Test
    fun `laporan hilang tidak bisa diklaim`() = runTest {
        val fixture = fixture("rpt-001")
        startCollecting(fixture.viewModel)
        runCurrent()

        assertFalse(fixture.viewModel.canClaim.value)
    }

    @Test
    fun `laporan temuan milik sendiri tidak bisa diklaim`() = runTest {
        val fixture = fixture("rpt-009", reportsWithOwnFoundItem())
        startCollecting(fixture.viewModel)
        runCurrent()

        assertFalse(fixture.viewModel.canClaim.value)

        fixture.viewModel.submitClaim("Barang ini saya sendiri yang titipkan ke pos")
        runCurrent()

        assertEquals(ClaimUiText.OWN_REPORT, fixture.viewModel.message.value)
        assertTrue(fixture.claims.observeClaims().first().none { it.reportId == "rpt-009" })
    }

    @Test
    fun `klaim botol yang sudah diajukan tidak bisa dikirim ulang`() = runTest {
        val fixture = fixture("rpt-007")
        startCollecting(fixture.viewModel)
        runCurrent()

        assertEquals(ClaimStatus.MENUNGGU, fixture.viewModel.myClaim.value?.status)
        assertFalse(fixture.viewModel.canClaim.value)

        fixture.viewModel.submitClaim("Tutup botol penyok di pinggir dan ada nama di bawah")
        runCurrent()

        assertEquals(ClaimUiText.ALREADY_SUBMITTED, fixture.viewModel.message.value)
        assertEquals(4, fixture.claims.observeClaims().first().size)
    }

    @Test
    fun `kirim klaim menyimpan ciri barang dan mengganti tombol jadi status`() = runTest {
        val fixture = fixture("rpt-005")
        startCollecting(fixture.viewModel)
        runCurrent()

        fixture.viewModel.submitClaim("Gagang kayu ada inisial R dan tali hitam di ujung")
        runCurrent()

        val claim = fixture.claims.observeClaims().first()
            .single { it.reportId == "rpt-005" && it.claimantId == DummyData.student.id }
        assertEquals(ClaimStatus.MENUNGGU, claim.status)
        assertEquals("Gagang kayu ada inisial R dan tali hitam di ujung", claim.answer)
        assertEquals(DummyData.student.fullName, claim.claimantName)
        assertEquals(ClaimUiText.SUBMITTED, fixture.viewModel.message.value)
        assertNotNull(fixture.viewModel.myClaim.value)
        assertFalse(fixture.viewModel.canClaim.value)
    }

    @Test
    fun `ciri barang terlalu pendek tidak dikirim`() = runTest {
        val fixture = fixture("rpt-005")
        startCollecting(fixture.viewModel)
        runCurrent()

        fixture.viewModel.submitClaim("biru")
        runCurrent()

        assertEquals(ClaimUiText.ANSWER_TOO_SHORT, fixture.viewModel.message.value)
        assertTrue(
            fixture.claims.observeClaims().first()
                .none { it.reportId == "rpt-005" && it.claimantId == DummyData.student.id }
        )
        assertTrue(fixture.viewModel.canClaim.value)
    }

    @Test
    fun `kandidat temuan yang sudah diverifikasi tetap disarankan`() = runTest {
        val fixture = fixture("rpt-001")
        val job = backgroundScope.launch(dispatcher) { fixture.viewModel.candidates.collect {} }
        runCurrent()

        assertEquals(listOf("rpt-007"), fixture.viewModel.candidates.value.map { it.id })

        job.cancel()
    }

    @Test
    fun `kandidat temuan yang belum diverifikasi tidak disarankan`() = runTest {
        // rpt-007 sengaja dibuat masih DILAPORKAN: belum boleh tampil di Beranda,
        // jadi belum pantas muncul sebagai "Mungkin barangmu" bagi pemilik.
        val pendingFound = DummyData.reports().first { it.id == "rpt-007" }
            .copy(status = ReportStatus.DILAPORKAN)
        val matches = DummyData.matches().map { match ->
            if (match.foundReport.id == "rpt-007") match.copy(foundReport = pendingFound) else match
        }

        val fixture = fixture("rpt-001", matches = matches)
        val job = backgroundScope.launch(dispatcher) { fixture.viewModel.candidates.collect {} }
        runCurrent()

        assertTrue(fixture.viewModel.candidates.value.isEmpty())

        job.cancel()
    }
}
