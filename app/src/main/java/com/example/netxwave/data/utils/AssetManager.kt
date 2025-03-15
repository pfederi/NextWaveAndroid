package com.example.nextwave.data.utils

import android.content.Context
import com.example.nextwave.data.models.Lake
import com.example.nextwave.data.models.LakeDeserializer
import com.example.nextwave.data.models.LakeStation
import com.example.nextwave.data.models.LakesData
import com.example.nextwave.data.models.Station
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.UUID

/**
 * Utility class to load data from assets
 */
object AssetManager {
    
    /**
     * Load JSON file from assets and parse it to the specified type
     */
    inline fun <reified T> loadJsonFromAssets(context: Context, fileName: String): T? {
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val gson = Gson()
            val type = object : TypeToken<T>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Load stations from assets
     */
    fun loadStationsFromAssets(context: Context): List<Station> {
        try {
            val jsonString = context.assets.open("stations.json").bufferedReader().use { it.readText() }
            
            // Create a custom Gson instance with our deserializer
            val gson = GsonBuilder()
                .registerTypeAdapter(Lake::class.java, LakeDeserializer())
                .create()
            
            val lakesData = gson.fromJson(jsonString, LakesData::class.java)
            
            // Convert to Station objects
            val stations = mutableListOf<Station>()
            
            lakesData.lakes.forEach { lake ->
                lake.stations.forEach { stationObj ->
                    when (stationObj) {
                        is String -> {
                            // It's a String station name
                            stations.add(
                                Station(
                                    id = UUID.randomUUID().toString(),
                                    name = stationObj,
                                    latitude = 0.0,
                                    longitude = 0.0,
                                    city = "",
                                    type = "",
                                    lake = lake.name,
                                    waveRating = calculateWaveRating(lake.name),
                                    description = generateDescription(lake.name)
                                )
                            )
                        }
                        is LakeStation -> {
                            // It's a LakeStation object
                            stations.add(
                                Station(
                                    id = stationObj.uic_ref,
                                    name = stationObj.name,
                                    latitude = stationObj.coordinates.latitude,
                                    longitude = stationObj.coordinates.longitude,
                                    city = extractCity(stationObj.name),
                                    type = determineType(stationObj.name),
                                    lake = lake.name,
                                    waveRating = calculateWaveRating(lake.name),
                                    description = generateDescription(lake.name)
                                )
                            )
                        }
                        else -> {
                            // Handle as a Map for flexibility
                            val map = stationObj as? Map<*, *>
                            if (map != null) {
                                val name = map["name"] as? String ?: ""
                                val uicRef = map["uic_ref"] as? String ?: UUID.randomUUID().toString()
                                
                                val coordinatesMap = map["coordinates"] as? Map<*, *>
                                val latitude = coordinatesMap?.get("latitude") as? Double ?: 0.0
                                val longitude = coordinatesMap?.get("longitude") as? Double ?: 0.0
                                
                                stations.add(
                                    Station(
                                        id = uicRef,
                                        name = name,
                                        latitude = latitude,
                                        longitude = longitude,
                                        city = extractCity(name),
                                        type = determineType(name),
                                        lake = lake.name,
                                        waveRating = calculateWaveRating(lake.name),
                                        description = generateDescription(lake.name)
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            return stations
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Extract city from station name
     */
    private fun extractCity(stationName: String): String {
        // Try to extract city from station name
        val parts = stationName.split(" ")
        return if (parts.size > 1) {
            // Return first part as city name
            parts[0]
        } else {
            stationName
        }
    }
    
    /**
     * Determine station type based on name
     */
    private fun determineType(stationName: String): String {
        return when {
            stationName.contains("Hafen", ignoreCase = true) -> "Harbor"
            stationName.contains("Schifflände", ignoreCase = true) -> "Main Terminal"
            stationName.contains("Landungssteg", ignoreCase = true) -> "Dock"
            stationName.contains("débarcadère", ignoreCase = true) -> "Terminal"
            stationName.contains("bateau", ignoreCase = true) -> "Terminal"
            stationName.contains("See", ignoreCase = true) -> "Terminal"
            stationName.contains("lac", ignoreCase = true) -> "Terminal"
            else -> "Stop"
        }
    }
    
    /**
     * Calculate wave rating based on lake
     */
    private fun calculateWaveRating(lakeName: String): Int {
        return when (lakeName) {
            "Vierwaldstättersee", "Thunersee", "Genfersee", "Lac Léman" -> (3..5).random()
            "Zürichsee", "Bodensee", "Brienzersee" -> (2..4).random()
            else -> (1..3).random()
        }
    }
    
    /**
     * Generate a description for the station
     */
    private fun generateDescription(lakeName: String): String {
        val descriptions = mapOf(
            "Zürichsee" to "Beliebter Spot am Zürichsee mit guten Wellen bei Südwestwind.",
            "Vierwaldstättersee" to "Malerischer Ort am Vierwaldstättersee mit ausgezeichneten Wellen bei Föhn.",
            "Bodensee" to "Schöne Lage am Bodensee mit mittleren Wellen und internationaler Atmosphäre.",
            "Lac Léman" to "Wunderschöner Ort am Genfersee mit hervorragenden Wellen bei Nordwind.",
            "Thunersee" to "Spektakuläre Alpenkulisse am Thunersee mit guten Wellenbedingungen.",
            "Brienzersee" to "Ruhiger Ort am türkisblauen Brienzersee mit moderaten Wellen.",
            "Lago Maggiore" to "Mediterranes Flair am Lago Maggiore mit angenehmen Wellenbedingungen.",
            "Lago di Lugano" to "Idyllischer Ort am Luganersee mit südlichem Charme und sanften Wellen.",
            "Bielersee" to "Charmante Lage am Bielersee mit guten Wellenbedingungen für Anfänger.",
            "Neuenburgersee" to "Weitläufiger See mit guten Windverhältnissen und moderaten Wellen.",
            "Murtensee" to "Historischer Ort am kleinen Murtensee mit ruhigen Gewässern.",
            "Aare" to "Flusslage mit interessanten Strömungsverhältnissen.",
            "Zugersee" to "Malerischer Ort am Zugersee mit mittleren Wellen.",
            "Walensee" to "Beeindruckende Bergkulisse am Walensee mit oft starken Winden.",
            "Hallwilersee" to "Idyllischer kleiner See mit sanften Wellen, ideal für Anfänger.",
            "Aegerisee" to "Ruhiger Bergsee mit gemäßigten Wellenbedingungen.",
            "Lac de Joux" to "Höchstgelegener Schifffahrtssee der Schweiz mit speziellen Windverhältnissen."
        )
        
        return descriptions[lakeName] ?: "Schöne Lage mit guten Wellenbedingungen."
    }
}