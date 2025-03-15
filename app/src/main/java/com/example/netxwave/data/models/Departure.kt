package com.example.nextwave.data.models

/**
 * Data model for a ship departure
 */
data class Departure(
    val time: String,           // Departure time (e.g. "16:28")
    val waveNumber: Int,        // Wave number (e.g. 11)
    val journeyNumber: String,  // Journey number (e.g. "38")
    val destination: String,    // Destination (e.g. "Küsnacht ZH")
    val status: DepartureStatus, // Status of the departure (missed, now, planned)
    val nextStation: String = ""  // Next station on the route
)

/**
 * Status of a departure
 */
enum class DepartureStatus {
    MISSED,  // Missed
    NOW,     // Now
    PLANNED  // Planned (not yet shown in the screenshot)
}