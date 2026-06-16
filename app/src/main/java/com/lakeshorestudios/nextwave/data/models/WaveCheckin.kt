package com.lakeshorestudios.nextwave.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
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
}
