package com.lakeshorestudios.nextwave.data.models

/**
 * Data model for a ship departure
 */
data class Departure(
    val time: String,           // Departure time (e.g. "16:28")
    val waveNumber: Int,        // Wave number (e.g. 11)
    val journeyNumber: String,  // Journey number (e.g. "38")
    val destination: String,    // Destination (e.g. "Küsnacht ZH")
    val status: DepartureStatus, // Status of the departure (missed, now, planned)
    val nextStation: String = "",  // Next station on the route
    val shipName: String? = null   // Ship name (ZSG only, e.g. "MS Albis")
)

/**
 * Wave rating based on ship type (larger ships = bigger waves)
 */
enum class WaveRating(val label: String, val waves: Int) {
    TIER1("Large waves", 3),   // MS Panta Rhei, MS Albis, EMS Uetliberg, EMS Pfannenstiel
    TIER2("Medium waves", 2),  // MS Wädenswil, MS Limmat, MS Helvetia, MS Linth, DS Stadt Zürich/Rapperswil
    TIER3("Small waves", 1);   // All other ships

    companion object {
        fun forShip(shipName: String): WaveRating {
            val name = shipName.trim()
            return when (name) {
                "MS Panta Rhei", "MS Albis",
                "EMS Uetliberg", "EMS Pfannenstiel",
                "EM Uetliberg", "EM Pfannenstiel" -> TIER1

                "MS Wädenswil", "MS Limmat", "MS Helvetia", "MS Linth",
                "DS Stadt Zürich", "DS Stadt Rapperswil" -> TIER2

                else -> TIER3
            }
        }
    }
}

/**
 * Status of a departure
 */
enum class DepartureStatus {
    MISSED,  // Missed
    NOW,     // Now
    PLANNED  // Planned (not yet shown in the screenshot)
}