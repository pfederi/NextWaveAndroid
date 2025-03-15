package com.example.netxwave.data.api

import com.example.netxwave.NextWaveApplication
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton class for creating and providing Retrofit API service
 */
object ApiClient {
    
    private const val TIMEOUT_SECONDS = 30L
    
    /**
     * Create and configure OkHttpClient
     */
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
    
    /**
     * Create Gson converter with date handling
     */
    private val gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }
    
    /**
     * Create Retrofit instance
     */
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NextWaveApplication.BASE_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Create API service
     */
    val apiService: NextWaveApiService by lazy {
        retrofit.create(NextWaveApiService::class.java)
    }
} 