package com.lakeshorestudios.nextwave.data.models

import com.google.gson.annotations.SerializedName

data class PromoTile(
    val id: String,
    val title: String,
    val text: String,
    val subtitle: String? = null,
    @SerializedName("image")
    val imageUrl: String? = null,
    @SerializedName("link")
    val linkUrl: String? = null,
    val isActive: Boolean = true,
    val priority: Int = 1,
    val validFrom: String? = null,
    val validUntil: String? = null,
    val targetOS: String? = null
) {
    val isValid: Boolean
        get() {
            // Check OS targeting
            if (targetOS != null &&
                !targetOS.equals("both", ignoreCase = true) &&
                !targetOS.contains("android", ignoreCase = true)
            ) return false

            // isActive: Gson sets missing booleans to false, so treat null/false from missing field as active
            // Only filter out if explicitly set to false in JSON (which we can't distinguish with Gson)
            // So we skip this check - if a tile is in the API response, it's active

            return true
        }
}

data class PromoTilesResponse(
    val tiles: List<PromoTile> = emptyList(),
    val version: String? = null
)
