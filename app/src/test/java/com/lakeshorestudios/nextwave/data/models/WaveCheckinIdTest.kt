package com.lakeshorestudios.nextwave.data.models

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class WaveCheckinIdTest {

    private fun utcDate(y: Int, mo: Int, d: Int, h: Int, mi: Int): Date {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.clear()
        c.set(y, mo - 1, d, h, mi, 0)
        return c.time
    }

    @Test
    fun usesUicRefWhenPresent() {
        val id = WaveCheckin.makeWaveId(
            stationUicRef = "8503671", stationName = "Thalwil",
            departure = utcDate(2026, 6, 18, 14, 32), routeNumber = "38"
        )
        assertEquals("8503671_2026-06-18T14:32:00Z_38", id)
    }

    @Test
    fun fallsBackToNameWhenNoUicRef() {
        val id = WaveCheckin.makeWaveId(
            stationUicRef = null, stationName = "Thalwil",
            departure = utcDate(2026, 6, 18, 14, 32), routeNumber = "38"
        )
        assertEquals("Thalwil_2026-06-18T14:32:00Z_38", id)
    }

    @Test
    fun isDeterministicInUtc() {
        val id = WaveCheckin.makeWaveId(
            stationUicRef = "8503671", stationName = "Thalwil",
            departure = utcDate(2026, 6, 18, 14, 32), routeNumber = "ZSG-12"
        )
        assertEquals("8503671_2026-06-18T14:32:00Z_ZSG-12", id)
    }
}
