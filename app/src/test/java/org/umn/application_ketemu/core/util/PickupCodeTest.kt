package org.umn.application_ketemu.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupCodeTest {

    @Test
    fun `kode selalu enam karakter dari alfabet aman`() {
        val code = PickupCode.generate("clm-001")
        assertEquals(6, code.length)
        assertTrue(code.all { it in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" })
    }

    @Test
    fun `kode deterministik untuk seed yang sama`() {
        assertEquals(PickupCode.generate("clm-001"), PickupCode.generate("clm-001"))
    }

    @Test
    fun `seed berbeda menghasilkan kode berbeda`() {
        assertNotEquals(PickupCode.generate("clm-001"), PickupCode.generate("clm-002"))
    }
}
