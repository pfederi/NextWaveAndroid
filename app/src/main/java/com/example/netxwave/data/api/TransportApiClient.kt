package com.example.nextwave.data.api

import com.example.nextwave.data.models.Departure
import com.example.nextwave.data.models.DepartureStatus
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * API errors for the Transport API
 */
sealed class TransportApiError(override val message: String) : Exception(message) {
    object InvalidUrl : TransportApiError("Invalid URL - Please contact support")
    object InvalidResponse : TransportApiError("The OpenTransport API is currently unavailable. Please try again later.")
    object NoJourneyFound : TransportApiError("No connections found")
    class NetworkError(errorMessage: String) : TransportApiError("Connection problem: $errorMessage")
    object Timeout : TransportApiError("The OpenTransport API (transport.opendata.ch) is not responding (Timeout).\n\nPossible reasons:\n• Server overloaded\n• Server maintenance\n• Temporary API disruption\n\nPlease try again in a few minutes.")
}

/**
 * Interface for the Transport API
 */
interface TransportApiService {
    @GET("stationboard")
    suspend fun getStationboard(
        @Query("id") stationId: String,
        @Query("limit") limit: Int = 50,
        @Query("date") date: String,
        @Query("time") time: String? = null,
        @Query("transportations[]") transportations: String = "ship"
    ): StationboardResponse
}

/**
 * Custom deserializer for Journey to fix the operator field issue
 */
class JourneyDeserializer : JsonDeserializer<Journey> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Journey {
        val jsonObject = json.asJsonObject
        
        // Extract fields from the JSON object
        val stop = context.deserialize<Stop>(jsonObject.get("stop"), Stop::class.java)
        val name = if (jsonObject.has("name") && !jsonObject.get("name").isJsonNull) jsonObject.get("name").asString else null
        val category = jsonObject.get("category").asString
        val number = if (jsonObject.has("number") && !jsonObject.get("number").isJsonNull) jsonObject.get("number").asString else null
        val to = if (jsonObject.has("to") && !jsonObject.get("to").isJsonNull) jsonObject.get("to").asString else null
        
        // Handle the operator field, which can be either a string or an object
        val operator: Operator? = if (jsonObject.has("operator")) {
            val operatorElement = jsonObject.get("operator")
            if (operatorElement.isJsonObject) {
                // If it's an object, deserialize it as Operator
                context.deserialize(operatorElement, Operator::class.java)
            } else if (operatorElement.isJsonPrimitive && operatorElement.asJsonPrimitive.isString) {
                // If it's a string, create an Operator object with the string as name
                Operator(name = operatorElement.asString)
            } else {
                null
            }
        } else {
            null
        }
        
        // Deserialize passList if present
        val passList: List<Stop>? = if (jsonObject.has("passList") && !jsonObject.get("passList").isJsonNull) {
            context.deserialize(jsonObject.get("passList"), object : TypeToken<List<Stop>>() {}.type)
        } else {
            null
        }
        
        // Extract capacity fields if present
        val capacity1st = if (jsonObject.has("capacity1st") && !jsonObject.get("capacity1st").isJsonNull) 
            jsonObject.get("capacity1st").asString else null
        val capacity2nd = if (jsonObject.has("capacity2nd") && !jsonObject.get("capacity2nd").isJsonNull) 
            jsonObject.get("capacity2nd").asString else null
        
        // Create and return the Journey object
        return Journey(
            stop = stop,
            name = name,
            category = category,
            number = number,
            operator = operator,
            to = to,
            passList = passList,
            capacity1st = capacity1st,
            capacity2nd = capacity2nd
        )
    }
}

/**
 * Client for the Swiss Transport API
 */
class TransportApiClient {
    
    companion object {
        private const val BASE_URL = "https://transport.opendata.ch/v1/"
        private const val TIMEOUT_SECONDS = 30L
        
        // Singleton instance
        @Volatile
        private var instance: TransportApiClient? = null
        
        fun getInstance(): TransportApiClient {
            return instance ?: synchronized(this) {
                instance ?: TransportApiClient().also { instance = it }
            }
        }
    }
    
    // OkHttpClient for network requests
    private val okHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    // Gson converter for JSON parsing with custom deserializer
    private val gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .registerTypeAdapter(Journey::class.java, JourneyDeserializer())
            .create()
    }
    
    // Retrofit instance
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    // API service
    private val apiService: TransportApiService by lazy {
        retrofit.create(TransportApiService::class.java)
    }
    
    /**
     * Retrieves departures for a specific station
     * @param stationId The ID of the station (UIC reference)
     * @param date The date for which to retrieve departures
     * @return A list of departures
     */
    suspend fun getDepartures(stationId: String, date: Date): List<Departure> = withContext(Dispatchers.IO) {
        try {
            // Format the date for the API
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(date)
            val formattedTime = timeFormat.format(date)
            
            // Retrieve the stationboard data
            val response = apiService.getStationboard(
                stationId = stationId,
                date = formattedDate,
                time = formattedTime
            )
            
            // Log the API response
            android.util.Log.d("TransportApiClient", "API Response: ${response.stationboard.size} journeys")
            response.stationboard.forEach { journey ->
                android.util.Log.d("TransportApiClient", "Journey: category=${journey.category}, number=${journey.number}, name=${journey.name}, to=${journey.to}")
                android.util.Log.d("TransportApiClient", "Operator: ${journey.operator?.name}, Stop: ${journey.stop.station.name}")
                if (journey.passList != null) {
                    android.util.Log.d("TransportApiClient", "PassList size: ${journey.passList.size}")
                }
            }
            
            // Filter for ship connections
            val filteredJourneys = response.stationboard.filter { journey ->
                journey.category == "BAT"
            }
            
            // Current time for status calculation
            val currentTime = Calendar.getInstance()
            
            // Convert the journeys to departures
            return@withContext filteredJourneys.map { journey ->
                // Extract the departure time
                val departureTime = journey.stop.departure?.let { departureStr ->
                    val departureFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
                    departureFormat.parse(departureStr)
                } ?: Date()
                
                val departureCalendar = Calendar.getInstance().apply { time = departureTime }
                
                // Calculate the status based on the current time
                val status = when {
                    departureCalendar.before(currentTime) -> DepartureStatus.MISSED
                    departureCalendar.timeInMillis - currentTime.timeInMillis < TimeUnit.MINUTES.toMillis(5) -> DepartureStatus.NOW
                    else -> DepartureStatus.PLANNED
                }
                
                // Simplified wave number: Use a default value
                val waveNumber = 0 // Will be overwritten in the UI later
                
                // Format the departure time
                val timeString = timeFormat.format(departureTime)
                
                // Format the journey number (remove leading zeros)
                val journeyNumber = journey.name?.let {
                    it.trimStart('0').ifEmpty { "0" } // If only zeros, keep a "0"
                } ?: ""
                
                // Extract the next station from the passList, if available
                val nextStation = if (journey.passList != null && journey.passList.size >= 2) {
                    // Simplified logic: Just use the second station in the list
                    val stationName = journey.passList[1].station.name
                    android.util.Log.d("TransportApiClient", "Using simplified logic - second station: $stationName")
                    stationName
                } else {
                    // Fallback: Use the final destination
                    android.util.Log.d("TransportApiClient", "No passList with at least 2 stations, using destination: ${journey.to}")
                    journey.to ?: "Unknown"
                }
                
                Departure(
                    time = timeString,
                    waveNumber = waveNumber,
                    journeyNumber = journeyNumber, // Use the formatted journey number
                    destination = journey.to ?: "Unknown",
                    status = status,
                    nextStation = nextStation
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            when (e) {
                is java.net.SocketTimeoutException -> throw TransportApiError.Timeout
                is java.net.UnknownHostException -> throw TransportApiError.NetworkError("No internet connection. Please check your connection and try again.")
                is retrofit2.HttpException -> {
                    if (e.code() == 404) {
                        throw TransportApiError.NoJourneyFound
                    } else {
                        throw TransportApiError.InvalidResponse
                    }
                }
                else -> throw TransportApiError.NetworkError("Unexpected error: ${e.message}")
            }
        }
    }
}

// Data models for the Transport API

data class StationboardResponse(
    val station: StationInfo,
    val stationboard: List<Journey>
)

data class StationInfo(
    val id: String,
    val name: String,
    val score: Int? = null,
    val coordinate: Coordinate? = null,
    val distance: Int? = null
)

data class Coordinate(
    val type: String,
    val x: Double, // longitude
    val y: Double  // latitude
)

data class Journey(
    val stop: Stop,
    val name: String? = null,
    val category: String,
    val number: String? = null,
    val operator: Operator? = null,
    val to: String? = null,
    val passList: List<Stop>? = null,
    val capacity1st: String? = null,
    val capacity2nd: String? = null
)

data class Stop(
    val station: StationInfo,
    val arrival: String? = null,
    val arrivalTimestamp: Long? = null,
    val departure: String? = null,
    val departureTimestamp: Long? = null,
    val delay: Int? = null,
    val platform: String? = null,
    val prognosis: Prognosis? = null
)

data class Operator(
    val id: String? = null,
    val name: String,
    val url: String? = null
)

data class Prognosis(
    val platform: String? = null,
    val arrival: String? = null,
    val departure: String? = null,
    val capacity1st: String? = null,
    val capacity2nd: String? = null
) 