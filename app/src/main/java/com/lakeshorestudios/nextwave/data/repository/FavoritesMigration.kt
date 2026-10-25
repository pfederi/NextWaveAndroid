package com.lakeshorestudios.nextwave.data.repository

import com.lakeshorestudios.nextwave.data.models.Station

/**
 * Migrates favorites that were saved with wrong station data from older versions of
 * stations.json. Favorites store whole Station objects (id = UIC), so fixing the asset
 * alone does not repair stations a user had already marked as favorite.
 *
 * Idempotent: a station is only touched when both its name and its old id match.
 */
object FavoritesMigration {

    private data class StationFix(
        val oldName: String,
        val matchesOldId: (String) -> Boolean,
        val newName: String,
        val newId: String,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    private val fixes = listOf(
        // Had Meilen's UIC
        StationFix(
            oldName = "Herrliberg (See)",
            matchesOldId = { it == "8503661" },
            newName = "Herrliberg (See)",
            newId = "8503660"
        ),
        // Was a copy of Altenrhein Schifflände
        StationFix(
            oldName = "Lindau (Bodensee)",
            matchesOldId = { it == "8506467" },
            newName = "Lindau (Bodensee)",
            newId = "8014655",
            latitude = 47.54378,
            longitude = 9.682267
        ),
        // Was a bare string without UIC, so it got a random UUID as id
        StationFix(
            oldName = "Interlaken Ost (See)",
            matchesOldId = { id -> id != "8508370" && !id.all { it.isDigit() } },
            newName = "Interlaken Ost (See)",
            newId = "8508370",
            latitude = 46.691693,
            longitude = 7.869188
        ),
        // Had Eierhals (Ägerisee) UIC and coordinates
        StationFix(
            oldName = "Le Lieu Roche Fendue (lac)",
            matchesOldId = { it == "8530633" },
            newName = "Le Lieu Roche Fendue (lac)",
            newId = "8511302",
            latitude = 46.644832,
            longitude = 6.28465
        ),
        // Had its name as uic_ref and a trailing space in the name
        StationFix(
            oldName = "Engelberg-Wingreis ",
            matchesOldId = { it == "Engelberg-Wingreis" },
            newName = "Engelberg-Wingreis",
            newId = "8504373"
        ),
        // Name was missing the closing parenthesis
        StationFix(
            oldName = "Le Rocheray (lac",
            matchesOldId = { it == "8530622" },
            newName = "Le Rocheray (lac)",
            newId = "8530622"
        )
    )

    /**
     * Returns the migrated list, or null if nothing had to be changed.
     */
    fun migrate(favorites: List<Station>): List<Station>? {
        var changed = false
        val migrated = favorites.map { station ->
            val fix = fixes.firstOrNull { it.oldName == station.name && it.matchesOldId(station.id) }
            if (fix == null) {
                station
            } else {
                changed = true
                station.copy(
                    id = fix.newId,
                    name = fix.newName,
                    latitude = fix.latitude ?: station.latitude,
                    longitude = fix.longitude ?: station.longitude
                )
            }
        }
        return if (changed) migrated.distinctBy { it.id } else null
    }
}
