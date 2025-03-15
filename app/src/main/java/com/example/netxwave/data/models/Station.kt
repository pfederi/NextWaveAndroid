package com.example.nextwave.data.models

/**
 * Data class representing a station (dock/port)
 */
data class Station(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val type: String,
    val lake: String = "",
    val waveRating: Int = 0,
    val description: String = ""
)

/**
 * Enum representing different types of stations
 */
enum class StationType {
    MAIN_PORT,
    SECONDARY_PORT,
    DOCK,
    STOP
} 