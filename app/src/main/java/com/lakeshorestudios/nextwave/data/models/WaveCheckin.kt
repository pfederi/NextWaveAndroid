package com.lakeshorestudios.nextwave.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Aggregated check-in count for one wave (matches the wave_checkin_counts RPC rows). */
@Serializable
data class WaveCheckinCount(
    @SerialName("wave_id") val waveId: String,
    val count: Int,
    val names: List<String> = emptyList()
)

object WaveCheckin {

    /**
     * Deterministic, cross-device wave identity. MUST stay byte-identical to the iOS
     * implementation (WaveCheckin.makeWaveId) so counts aggregate across platforms.
     * Format: {uicRef ?? name}_{yyyy-MM-dd'T'HH:mm:ss'Z' in UTC}_{routeNumber}
     */
    fun makeWaveId(
        stationUicRef: String?,
        stationName: String,
        departure: Date,
        routeNumber: String
    ): String {
        val station = stationUicRef ?: stationName
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(departure)
        return "${station}_${iso}_${routeNumber}"
    }

    /** Calendar key (year + day-of-year) in Europe/Zurich, so "the day" is the lake's local day. */
    private fun zurichDayKey(time: Date): Long {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Zurich"))
        c.time = time
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR)
    }

    private fun sameLocalDay(time: Date, among: List<Date>): List<Date> {
        val key = zurichDayKey(time)
        return among.filter { zurichDayKey(it) == key }
    }

    /** True if `time` is the earliest departure on its local (Europe/Zurich) calendar day among `among`. */
    fun isFirstOfDay(time: Date, amongDepartures: List<Date>): Boolean {
        val earliest = sameLocalDay(time, amongDepartures).minByOrNull { it.time } ?: return false
        return time.time == earliest.time
    }

    /** True if `time` is the latest departure on its local (Europe/Zurich) calendar day among `among`. */
    fun isLastOfDay(time: Date, amongDepartures: List<Date>): Boolean {
        val latest = sameLocalDay(time, amongDepartures).maxByOrNull { it.time } ?: return false
        return time.time == latest.time
    }
}
