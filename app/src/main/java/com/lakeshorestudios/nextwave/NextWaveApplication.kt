package com.example.netxwave

import android.app.Application

class NextWaveApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
    
    companion object {
        // App-wide constants
        const val BASE_API_URL = "https://api.nextwaveapp.ch/"
    }
} 