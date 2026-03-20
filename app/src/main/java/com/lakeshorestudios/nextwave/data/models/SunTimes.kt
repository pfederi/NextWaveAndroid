package com.lakeshorestudios.nextwave.data.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Data class representing sunrise/sunset and twilight times
 */
data class SunTimes(
    val sunrise: Date,
    val sunset: Date,
    val civilTwilightBegin: Date,
    val civilTwilightEnd: Date
) {
    /**
     * Determine the daylight phase for a given time
     */
    fun getDaylightPhase(time: Date): DaylightPhase {
        return when {
            time.before(civilTwilightBegin) -> DaylightPhase.NIGHT
            time.before(sunrise) -> DaylightPhase.TWILIGHT
            time.before(sunset) -> DaylightPhase.DAY
            time.before(civilTwilightEnd) -> DaylightPhase.TWILIGHT
            else -> DaylightPhase.NIGHT
        }
    }
}

/**
 * Daylight phase enum
 */
enum class DaylightPhase {
    DAY,
    TWILIGHT,
    NIGHT
}

/**
 * API response from sunrise-sunset.org
 */
data class SunTimesResponse(
    val results: SunTimesResults,
    val status: String
)

data class SunTimesResults(
    val sunrise: String,
    val sunset: String,
    @SerializedName("civil_twilight_begin")
    val civilTwilightBegin: String,
    @SerializedName("civil_twilight_end")
    val civilTwilightEnd: String
)
