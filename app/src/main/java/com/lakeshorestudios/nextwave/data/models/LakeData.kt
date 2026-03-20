package com.lakeshorestudios.nextwave.data.models

/**
 * Data class representing water temperature for a lake
 */
data class WaterTemperature(
    val lakeName: String,
    val temperature: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Data class representing water level for a lake
 */
data class WaterLevel(
    val lakeName: String,
    val level: String,
    val temperature: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Combined lake environmental data for UI display
 */
data class LakeEnvironmentData(
    val waterTemperature: Double? = null,
    val waterLevel: String? = null,
    val waterLevelDifference: String? = null,
    val sunTimes: SunTimes? = null
)

/**
 * Response from the Alplakes API for current temperature (profile endpoint)
 */
data class AlplakesProfileResponse(
    val time: String? = null,
    val depth: List<Double>? = null,
    val temperature: List<Double>? = null
)

/**
 * Response from the VesselData water-temperature API
 */
data class WaterLevelResponse(
    val lakes: List<WaterLevelLake>? = null
)

data class WaterLevelLake(
    val name: String,
    val level: String? = null,
    val temperature: String? = null
)

/**
 * Wetsuit thickness recommendation based on water temperature.
 * Based on Quiksilver wetsuit thickness guide.
 * Rule: if air + water < 30°C, go one size thicker.
 * Returns e.g. "3/2" or null if too warm for a wetsuit.
 */
fun getWetsuitThickness(waterTemp: Double, airTemp: Double? = null): String? {
    var adjusted = waterTemp

    // If air + water < 30°C, simulate lower water temp for thicker suit
    if (airTemp != null && (airTemp + waterTemp) < 30) {
        adjusted = waterTemp - 3
    }

    return when {
        adjusted >= 23 -> null      // Too warm
        adjusted >= 18 -> "0.5-2"
        adjusted >= 15 -> "3/2"
        adjusted >= 12 -> "4/3"
        adjusted >= 10 -> "5/4"
        else -> "6/5"
    }
}

/**
 * Average water levels for Swiss lakes (in meters above sea level)
 * Used to calculate water level difference
 */
object AverageWaterLevels {
    private val levels = mapOf(
        "Zürichsee" to 405.94,
        "Vierwaldstättersee" to 433.60,
        "Bodensee" to 395.60,
        "Genfersee" to 372.00,
        "Lac Léman" to 372.00,
        "Thunersee" to 557.80,
        "Brienzersee" to 563.70,
        "Bielersee" to 429.30,
        "Neuenburgersee" to 429.30,
        "Murtensee" to 429.30,
        "Zugersee" to 413.50,
        "Walensee" to 419.20,
        "Hallwilersee" to 449.30,
        "Sempachersee" to 503.30,
        "Lago Maggiore" to 193.50
    )

    fun getAverage(lakeName: String): Double? = levels[lakeName]
}
