package org.umn.application_ketemu.data.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.application_ketemu.core.util.isCampusEmail
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.domain.repository.AuthErrorReason
import org.umn.application_ketemu.domain.repository.AuthResult

/**
 * Pengujian autentikasi lokal (tanpa jaringan).
 *
 * Memastikan login benar-benar memeriksa kata sandi terhadap akun dummy yang
 * sudah ada — bukan sekadar menerima email apa pun.
 */
class LocalAuthRepositoryTest {

    private val repository = LocalAuthRepository()

    @Test
    fun mahasiswaDenganNimSebagaiSandiBerhasilMasuk() = runTest {
        val result = repository.signIn(DummyData.student.email, DummyData.student.nimNidn)

        assertTrue(result is AuthResult.Success)
        assertEquals(
            DummyData.student.email,
            (result as AuthResult.Success).user?.email
        )
        assertEquals(UserRole.USER, result.user?.role)
    }

    @Test
    fun staffDenganNidnSebagaiSandiBerhasilMasuk() = runTest {
        val result = repository.signIn(DummyData.staff.email, DummyData.staff.nimNidn)

        assertTrue(result is AuthResult.Success)
        assertEquals(UserRole.STAFF, (result as AuthResult.Success).user?.role)
    }

    @Test
    fun emailDiberiSpasiDanHurufBesarTetapDiterima() = runTest {
        val result = repository.signIn(
            "  Budi.Santoso@STUDENT.UMN.ac.id ",
            DummyData.student.nimNidn
        )

        assertTrue(result is AuthResult.Success)
        assertEquals("budi.santoso@student.umn.ac.id", (result as AuthResult.Success).user?.email)
    }

    @Test
    fun kataSandiSalahDitolak() = runTest {
        val result = repository.signIn(DummyData.student.email, "salah123")

        assertTrue(result is AuthResult.Failure)
        assertEquals(
            AuthErrorReason.INVALID_CREDENTIALS,
            (result as AuthResult.Failure).reason
        )
    }

    @Test
    fun emailTidakTerdaftarDitolak() = runTest {
        val result = repository.signIn("orang.lain@umn.ac.id", "00000099999")

        assertTrue(result is AuthResult.Failure)
        assertEquals(
            AuthErrorReason.INVALID_CREDENTIALS,
            (result as AuthResult.Failure).reason
        )
    }

    @Test
    fun akunBaruDariPendaftaranLangsungDapatDipakaiMasuk() = runTest {
        val signUp = repository.signUp(
            fullName = "Siti Aminah",
            email = "siti.aminah@student.umn.ac.id",
            nimNidn = "00000111222",
            phone = "0812-3333-4444",
            password = "rahasiaSiti"
        )
        assertTrue(signUp is AuthResult.Success)
        assertNotNull((signUp as AuthResult.Success).user)

        val signIn = repository.signIn("siti.aminah@student.umn.ac.id", "rahasiaSiti")
        assertTrue(signIn is AuthResult.Success)
        assertEquals("Siti Aminah", (signIn as AuthResult.Success).user?.fullName)
    }

    @Test
    fun pendaftaranDenganEmailYangSudahAdaDitolak() = runTest {
        val result = repository.signUp(
            fullName = "Budi Kembar",
            email = DummyData.student.email,
            nimNidn = "00000099999",
            phone = "",
            password = "rahasia123"
        )

        assertTrue(result is AuthResult.Failure)
        assertEquals(
            AuthErrorReason.USER_ALREADY_EXISTS,
            (result as AuthResult.Failure).reason
        )
    }

    @Test
    fun daftarAkunUjiCobaBerisiDuaAkunDenganSandiNimNidn() {
        val accounts = LocalAuthRepository.TRIAL_ACCOUNTS

        assertEquals(2, accounts.size)
        accounts.forEach { account ->
            assertTrue(isCampusEmail(account.email, "umn.ac.id"))
            assertTrue(account.password.isNotBlank())
            assertTrue(account.roleLabel.isNotBlank())
        }
    }
}
