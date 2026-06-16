package com.lakeshorestudios.nextwave.data.utils

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ShareTextBuilderTest {

    private fun date(): java.util.Date {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Zurich"))
        c.set(2026, Calendar.JUNE, 18, 14, 32, 0)
        return c.time
    }

    @Test
    fun containsCoreLines() {
        val text = ShareTextBuilder.build(
            stationName = "Thalwil",
            destinationName = "Zürich",
            waveTime = date(),
            routeNumber = "38",
            shipName = "MS Wädenswil",
            airTemperature = 21.5,
            introIndex = 0
        )
        assertTrue(text.startsWith(ShareTextBuilder.INTROS[0]))
        assertTrue(text.contains("📍 Thalwil → Zürich"))
        assertTrue(text.contains("🕐 14:32 Uhr"))
        assertTrue(text.contains("🚢 Route 38"))
        assertTrue(text.contains("⛴️ MS Wädenswil"))
        assertTrue(text.contains("🌡️ 21.5°C"))
        assertTrue(text.contains("📱 Shared via NextWave App"))
    }

    @Test
    fun omitsOptionalLinesWhenNull() {
        val text = ShareTextBuilder.build(
            stationName = "Thalwil", destinationName = "Zürich",
            waveTime = date(), routeNumber = "38",
            shipName = null, airTemperature = null, introIndex = 1
        )
        assertTrue(!text.contains("⛴️"))
        assertTrue(!text.contains("🌡️"))
    }
}
