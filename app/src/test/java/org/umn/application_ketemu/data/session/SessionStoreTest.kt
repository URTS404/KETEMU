package org.umn.application_ketemu.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.domain.model.UserRole

/**
 * Pengujian penyimpan sesi — mengunci perbaikan bug:
 * "akun pertama hilang setelah berpindah-pindah peran".
 *
 * Yang dijamin: [SessionStore.switchRole] hanya mengubah field `role`,
 * sedangkan identitas akun (id, nama, email, NIM/NIDN) tetap utuh, dan kembali
 * ke USER memulihkan akun asli apa adanya.
 */
class SessionStoreTest {

    private val akunAsli = DummyData.student.copy(
        id = "user-aku-01",
        fullName = "Rani Kusuma",
        email = "rani.kusuma@student.umn.ac.id",
        nimNidn = "00000099999",
        role = UserRole.USER
    )

    @Test
    fun pindahPeranTidakMenghapusIdentitasAkun() {
        val session = SessionStore()
        session.signIn(akunAsli)

        session.switchRole(UserRole.STAFF)
        with(session.currentUser.value) {
            assertEquals("user-aku-01", id)
            assertEquals("Rani Kusuma", fullName)
            assertEquals("rani.kusuma@student.umn.ac.id", email)
            assertEquals("00000099999", nimNidn)
            assertEquals(UserRole.STAFF, role)
        }

        session.switchRole(UserRole.ADMIN)
        with(session.currentUser.value) {
            assertEquals("user-aku-01", id)
            assertEquals("Rani Kusuma", fullName)
            assertEquals("rani.kusuma@student.umn.ac.id", email)
            assertEquals(UserRole.ADMIN, role)
        }
    }

    @Test
    fun kembaliKePeranUserMemulihkanAkunAsli() {
        val session = SessionStore()
        session.signIn(akunAsli)

        session.switchRole(UserRole.STAFF)
        session.switchRole(UserRole.ADMIN)
        session.switchRole(UserRole.USER)

        assertEquals(akunAsli, session.currentUser.value)
    }

    @Test
    fun signInMembukaSesiDanSignOutTidakMenghapusProfil() {
        val session = SessionStore()
        session.signIn(akunAsli)
        assertEquals(true, session.isLoggedIn.value)

        session.signOut()
        assertFalse(session.isLoggedIn.value)
        assertEquals(akunAsli, session.currentUser.value)
    }
}
