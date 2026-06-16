package com.lakeshorestudios.nextwave.data.utils

/**
 * Pure, Android-free description of a calendar event for a wave.
 * All inputs are pre-resolved by the caller so this type stays testable.
 */
data class CalendarEventContent(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String
) {
    companion object {
        private const val ONE_HOUR_MILLIS = 3_600_000L

        fun make(
            waveTimeMillis: Long,
            stationName: String?,
            destinationName: String,
            latitude: Double?,
            longitude: Double?,
            shipName: String?,
            airTemperature: Double?,
            windKnots: Double?,
            windDirection: String?
        ): CalendarEventContent {
            val title = if (!stationName.isNullOrEmpty()) {
                "🌊 Wave – $stationName → $destinationName"
            } else {
                "🌊 Wave → $destinationName"
            }

            val lines = mutableListOf<String>()
            if (!shipName.isNullOrEmpty()) lines.add("⛴️ $shipName")
            if (airTemperature != null) lines.add("🌡️ ${String.format("%.1f°C", airTemperature)}")
            if (windKnots != null) {
                val dir = windDirection?.let { " $it" } ?: ""
                lines.add("💨 ${windKnots.toInt()} kn$dir")
            }
            lines.add("")
            lines.add("📱 Created with NextWave")

            return CalendarEventContent(
                title = title,
                startMillis = waveTimeMillis,
                endMillis = waveTimeMillis + ONE_HOUR_MILLIS,
                location = stationName?.ifEmpty { null },
                latitude = latitude,
                longitude = longitude,
                notes = lines.joinToString("\n")
            )
        }
    }
}
