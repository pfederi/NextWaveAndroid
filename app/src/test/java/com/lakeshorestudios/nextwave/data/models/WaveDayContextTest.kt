package com.lakeshorestudios.nextwave.data.models

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class WaveDayContextTest {

    /** Build a Date at a given Europe/Zurich wall-clock time. */
    private fun zurich(y: Int, mo: Int, d: Int, h: Int, mi: Int): Date {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Zurich"))
        c.clear()
        c.set(y, mo - 1, d, h, mi, 0)
        return c.time
    }

    @Test
    fun earliestSameDayDepartureIsFirst() {
        val day = listOf(
            zurich(2026, 6, 18, 7, 0),
            zurich(2026, 6, 18, 12, 0),
            zurich(2026, 6, 18, 19, 30)
        )
        assertTrue(WaveCheckin.isFirstOfDay(zurich(2026, 6, 18, 7, 0), day))
        assertFalse(WaveCheckin.isFirstOfDay(zurich(2026, 6, 18, 12, 0), day))
    }

    @Test
    fun latestSameDayDepartureIsLast() {
        val day = listOf(
            zurich(2026, 6, 18, 7, 0),
            zurich(2026, 6, 18, 19, 30)
        )
        assertTrue(WaveCheckin.isLastOfDay(zurich(2026, 6, 18, 19, 30), day))
        assertFalse(WaveCheckin.isLastOfDay(zurich(2026, 6, 18, 7, 0), day))
    }

    @Test
    fun singleDepartureIsBothFirstAndLast() {
        val only = zurich(2026, 6, 18, 9, 0)
        val day = listOf(only)
        assertTrue(WaveCheckin.isFirstOfDay(only, day))
        assertTrue(WaveCheckin.isLastOfDay(only, day))
    }

    @Test
    fun otherDaysDoNotAffectTheFlag() {
        // A later departure on the NEXT day must not make today's evening sailing "not last".
        val day = listOf(
            zurich(2026, 6, 18, 19, 0),
            zurich(2026, 6, 19, 6, 0)
        )
        assertTrue(WaveCheckin.isLastOfDay(zurich(2026, 6, 18, 19, 0), day))
        assertTrue(WaveCheckin.isFirstOfDay(zurich(2026, 6, 19, 6, 0), day))
    }

    @Test
    fun departureNotInListIsNeverFlagged() {
        val day = listOf(zurich(2026, 6, 18, 7, 0))
        assertFalse(WaveCheckin.isFirstOfDay(zurich(2026, 6, 18, 8, 0), day))
        assertFalse(WaveCheckin.isLastOfDay(zurich(2026, 6, 18, 8, 0), day))
    }
}
