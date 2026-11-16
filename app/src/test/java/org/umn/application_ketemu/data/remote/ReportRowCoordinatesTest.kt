package org.umn.application_ketemu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.umn.application_ketemu.domain.model.GeoPoint

class ReportRowCoordinatesTest {

    private fun row(
        latitude: Double? = null,
        longitude: Double? = null
    ) = ReportRow(
        id = "0f0e0d0c-0000-4000-8000-000000000001",
        ownerId = "0f0e0d0c-0000-4000-8000-000000000002",
        title = "Tumbler hitam",
        latitude = latitude,
        longitude = longitude,
        createdAt = "2026-11-16T08:00:00+07:00"
    )

    @Test
    fun `koordinat lengkap dipetakan ke domain`() {
        val report = row(latitude = -6.2577, longitude = 106.6182).toDomain()

        assertEquals(GeoPoint(latitude = -6.2577, longitude = 106.6182), report?.coordinates)
    }

    @Test
    fun `laporan tanpa koordinat tetap valid`() {
        val report = row().toDomain()

        assertEquals("Tumbler hitam", report?.title)
        assertNull(report?.coordinates)
    }

    @Test
    fun `koordinat setengah jadi diabaikan`() {
        assertNull(row(latitude = -6.2577).toDomain()?.coordinates)
        assertNull(row(longitude = 106.6182).toDomain()?.coordinates)
    }

    @Test
    fun `koordinat di luar rentang diabaikan`() {
        assertNull(row(latitude = 123.0, longitude = 106.6182).toDomain()?.coordinates)
        assertNull(row(latitude = -6.2577, longitude = 200.0).toDomain()?.coordinates)
    }
}
