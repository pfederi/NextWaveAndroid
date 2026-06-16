package com.lakeshorestudios.nextwave.data.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

/**
 * Pure builder for the human-readable share text (WhatsApp / SMS / Mail).
 * Port of the iOS generateShareText. Water temp / wetsuit / water level are
 * intentionally omitted (no Android data source yet).
 */
object ShareTextBuilder {

    val INTROS: List<String> = listOf(
        "🥳 Let's share the next wave for a party wave!",
        "🌊 Catch the wave with me!",
        "🌊 Ready to ride the next wave together?",
        "🚢 All aboard for the next adventure!",
        "🌊 Join me on this wave - it's going to be epic!"
    )

    fun build(
        stationName: String,
        destinationName: String,
        waveTime: Date,
        routeNumber: String,
        shipName: String?,
        airTemperature: Double?,
        introIndex: Int = Random.nextInt(INTROS.size)
    ): String {
        val dateFormatter = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale("de", "CH")).apply {
            timeZone = TimeZone.getTimeZone("Europe/Zurich")
        }
        val timeFormatter = SimpleDateFormat("HH:mm", Locale("de", "CH")).apply {
            timeZone = TimeZone.getTimeZone("Europe/Zurich")
        }

        val intro = INTROS[introIndex.coerceIn(0, INTROS.size - 1)]
        val sb = StringBuilder()
        sb.append(intro).append("\n\n")
        sb.append("📍 $stationName → $destinationName\n")
        sb.append("📅 ${dateFormatter.format(waveTime)}\n")
        sb.append("🕐 ${timeFormatter.format(waveTime)} Uhr\n")
        sb.append("🚢 Route $routeNumber\n")
        if (!shipName.isNullOrEmpty()) sb.append("⛴️ $shipName\n")
        if (airTemperature != null) sb.append("🌡️ ${String.format("%.1f°C", airTemperature)}\n")
        sb.append("\n📱 Shared via NextWave App\n")
        return sb.toString()
    }
}
