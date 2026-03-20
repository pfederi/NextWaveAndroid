package com.lakeshorestudios.nextwave.data.api

import android.util.Log
import com.google.gson.Gson
import com.lakeshorestudios.nextwave.data.models.PromoTile
import com.lakeshorestudios.nextwave.data.models.PromoTilesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lakeshorestudios.nextwave.data.utils.readTextWithTimeout
import java.net.URL

/**
 * API client for fetching promotional tiles.
 * Endpoint: https://www.nextwaveapp.ch/api/promo-tiles.json
 */
class PromoTileApiClient {

    @Volatile private var cachedTiles: List<PromoTile>? = null
    @Volatile private var cacheTime: Long = 0
    private val cacheValidityMs = 60 * 60 * 1000L // 1 hour

    suspend fun getPromoTiles(): List<PromoTile> = withContext(Dispatchers.IO) {
        try {
            // Check cache
            val cached = cachedTiles
            if (cached != null && System.currentTimeMillis() - cacheTime < cacheValidityMs) {
                return@withContext cached
            }

            val url = "https://www.nextwaveapp.ch/api/promo-tiles.json"
            Log.d(TAG, "Fetching promo tiles from: $url")

            val responseText = URL(url).readTextWithTimeout()
            val response = Gson().fromJson(responseText, PromoTilesResponse::class.java)

            val validTiles = response.tiles.filter { it.isValid }
            cachedTiles = validTiles
            cacheTime = System.currentTimeMillis()

            Log.d(TAG, "Fetched ${validTiles.size} valid promo tiles")
            validTiles
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching promo tiles: ${e.message}")
            cachedTiles ?: emptyList()
        }
    }

    companion object {
        private const val TAG = "PromoTileApiClient"

        @Volatile
        private var instance: PromoTileApiClient? = null

        fun getInstance(): PromoTileApiClient {
            return instance ?: synchronized(this) {
                instance ?: PromoTileApiClient().also { instance = it }
            }
        }
    }
}
