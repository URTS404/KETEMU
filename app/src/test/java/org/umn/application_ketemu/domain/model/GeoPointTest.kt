package org.umn.application_ketemu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GeoPointTest {

    @Test
    fun `titik kampus dianggap valid`() {
        assertTrue(GeoPoint.CAMPUS_CENTER.isValid)
    }

    @Test
    fun `batas rentang bumi masih diterima`() {
        assertTrue(GeoPoint(latitude = 90.0, longitude = 180.0).isValid)
        assertTrue(GeoPoint(latitude = -90.0, longitude = -180.0).isValid)
        assertTrue(GeoPoint(latitude = 0.0, longitude = 0.0).isValid)
    }

    @Test
    fun `koordinat di luar rentang ditolak`() {
        assertFalse(GeoPoint(latitude = 90.000001, longitude = 0.0).isValid)
        assertFalse(GeoPoint(latitude = -90.000001, longitude = 0.0).isValid)
        assertFalse(GeoPoint(latitude = 0.0, longitude = 180.000001).isValid)
        assertFalse(GeoPoint(latitude = 0.0, longitude = -180.000001).isValid)
    }

    @Test
    fun `nilai bukan angka atau tak hingga ditolak`() {
        assertFalse(GeoPoint(latitude = Double.NaN, longitude = 0.0).isValid)
        assertFalse(GeoPoint(latitude = 0.0, longitude = Double.NaN).isValid)
        assertFalse(GeoPoint(latitude = Double.POSITIVE_INFINITY, longitude = 0.0).isValid)
        assertFalse(GeoPoint(latitude = 0.0, longitude = Double.NEGATIVE_INFINITY).isValid)
    }

    @Test
    fun `teks koordinat memakai enam desimal dan pemisah titik`() {
        assertEquals("-6.257700, 106.618200", GeoPoint.CAMPUS_CENTER.toCoordinateText())
    }

    @Test
    fun `format koordinat tidak terpengaruh locale perangkat`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            // Di locale Jerman desimal default memakai koma; hasil harus tetap titik.
            assertEquals("-6.257700, 106.618200", GeoPoint.CAMPUS_CENTER.toCoordinateText())
        } finally {
            Locale.setDefault(original)
        }
    }
}
