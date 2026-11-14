package org.umn.application_ketemu.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRoleTest {

    @Test
    fun `hanya staff dan admin yang boleh membuka panel moderasi`() {
        assertFalse(UserRole.USER.canModerate)
        assertTrue(UserRole.STAFF.canModerate)
        assertTrue(UserRole.ADMIN.canModerate)
    }
}
