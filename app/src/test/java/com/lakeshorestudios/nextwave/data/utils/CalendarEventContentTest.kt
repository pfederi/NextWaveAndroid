package com.lakeshorestudios.nextwave.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventContentTest {

    @Test
    fun buildsTitleWithStation() {
        val c = CalendarEventContent.make(
            waveTimeMillis = 1_000_000L,
            stationName = "Thalwil",
            destinationName = "Zürich",
            latitude = null, longitude = null,
            shipName = null, airTemperature = null,
            windKnots = null, windDirection = null
        )
        assertEquals("🌊 Wave – Thalwil → Zürich", c.title)
        assertEquals(1_000_000L, c.startMillis)
        assertEquals(1_000_000L + 3_600_000L, c.endMillis)
    }

    @Test
    fun buildsTitleWithoutStation() {
        val c = CalendarEventContent.make(
            waveTimeMillis = 0L, stationName = "", destinationName = "Zürich",
            latitude = null, longitude = null, shipName = null,
            airTemperature = null, windKnots = null, windDirection = null
        )
        assertEquals("🌊 Wave → Zürich", c.title)
    }

    @Test
    fun notesContainWeatherAndShip() {
        val c = CalendarEventContent.make(
            waveTimeMillis = 0L, stationName = "Thalwil", destinationName = "Zürich",
            latitude = null, longitude = null,
            shipName = "MS Wädenswil", airTemperature = 21.5,
            windKnots = 12.0, windDirection = "NW"
        )
        assertTrue(c.notes.contains("⛴️ MS Wädenswil"))
        assertTrue(c.notes.contains("🌡️ 21.5°C"))
        assertTrue(c.notes.contains("💨 12 kn NW"))
        assertTrue(c.notes.contains("📱 Created with NextWave"))
    }
}
