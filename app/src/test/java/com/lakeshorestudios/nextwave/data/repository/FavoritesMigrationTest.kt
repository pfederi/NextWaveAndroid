package com.lakeshorestudios.nextwave.data.repository

import com.lakeshorestudios.nextwave.data.models.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoritesMigrationTest {

    private fun station(id: String, name: String, lat: Double = 1.0, lon: Double = 2.0) =
        Station(id = id, name = name, latitude = lat, longitude = lon, city = "", type = "")

    @Test
    fun `fixes Herrliberg and Lindau and keeps order`() {
        val favorites = listOf(
            station("8503651", "Zürich Bürkliplatz (See)"),
            station("8503661", "Herrliberg (See)", 47.283303, 8.6095295),
            station("8506467", "Lindau (Bodensee)", 47.497518, 9.561171)
        )

        val migrated = FavoritesMigration.migrate(favorites)!!

        assertEquals(listOf("8503651", "8503660", "8014655"), migrated.map { it.id })
        assertEquals(47.283303, migrated[1].latitude, 0.0)
        assertEquals(8.6095295, migrated[1].longitude, 0.0)
        assertEquals(47.54378, migrated[2].latitude, 0.0)
        assertEquals(9.682267, migrated[2].longitude, 0.0)
    }

    @Test
    fun `does not touch Meilen or Altenrhein sharing the old ids`() {
        val favorites = listOf(
            station("8503661", "Meilen (See)"),
            station("8506467", "Altenrhein Schifflände")
        )

        assertNull(FavoritesMigration.migrate(favorites))
    }

    @Test
    fun `is idempotent`() {
        val once = FavoritesMigration.migrate(
            listOf(station("8503661", "Herrliberg (See)"), station("8506467", "Lindau (Bodensee)"))
        )!!

        assertNull(FavoritesMigration.migrate(once))
    }

    @Test
    fun `fixes Engelberg-Wingreis id and trims name`() {
        val migrated = FavoritesMigration.migrate(
            listOf(station("Engelberg-Wingreis", "Engelberg-Wingreis ", 47.102154, 7.177546))
        )!!

        assertEquals("8504373", migrated.single().id)
        assertEquals("Engelberg-Wingreis", migrated.single().name)
        assertEquals(47.102154, migrated.single().latitude, 0.0)
    }

    @Test
    fun `gives Interlaken Ost its UIC instead of the random id`() {
        val migrated = FavoritesMigration.migrate(
            listOf(station("3f2c1a9e-0000-4000-8000-000000000000", "Interlaken Ost (See)", 0.0, 0.0))
        )!!

        assertEquals("8508370", migrated.single().id)
        assertEquals(46.691693, migrated.single().latitude, 0.0)
    }
}
