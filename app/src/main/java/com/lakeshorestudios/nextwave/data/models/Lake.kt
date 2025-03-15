package com.example.netxwave.data.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Data class representing a lake with its stations
 */
data class Lake(
    val name: String,
    val operators: List<String>,
    val stations: List<Any> // Changed from List<LakeStation> to List<Any> to handle mixed types
)

/**
 * Data class representing a station within a lake
 */
data class LakeStation(
    val name: String,
    val uic_ref: String,
    val coordinates: Coordinates
)

/**
 * Data class representing coordinates
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

/**
 * Data class representing the root of the JSON structure
 */
data class LakesData(
    val lakes: List<Lake>
)

/**
 * Custom deserializer for Lake to handle mixed types in stations list
 */
class LakeDeserializer : JsonDeserializer<Lake> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Lake {
        val jsonObject = json.asJsonObject
        val name = jsonObject.get("name").asString
        
        // Parse operators
        val operatorsArray = jsonObject.getAsJsonArray("operators")
        val operators = mutableListOf<String>()
        operatorsArray?.forEach { operators.add(it.asString) }
        
        // Parse stations (can be either String or LakeStation)
        val stationsArray = jsonObject.getAsJsonArray("stations")
        val stations = mutableListOf<Any>()
        
        stationsArray?.forEach { element ->
            if (element.isJsonPrimitive) {
                // It's a String
                stations.add(element.asString)
            } else {
                // It's a LakeStation
                val stationObj = element.asJsonObject
                val stationName = stationObj.get("name").asString
                val uicRef = stationObj.get("uic_ref").asString
                
                val coordinatesObj = stationObj.getAsJsonObject("coordinates")
                val latitude = coordinatesObj.get("latitude").asDouble
                val longitude = coordinatesObj.get("longitude").asDouble
                
                stations.add(
                    LakeStation(
                        name = stationName,
                        uic_ref = uicRef,
                        coordinates = Coordinates(latitude, longitude)
                    )
                )
            }
        }
        
        return Lake(name, operators, stations)
    }
} 