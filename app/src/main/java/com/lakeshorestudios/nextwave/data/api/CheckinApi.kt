package com.lakeshorestudios.nextwave.data.api

import com.lakeshorestudios.nextwave.data.models.WaveCheckinCount
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CheckinApi {

    @Serializable
    private data class CheckinRow(
        @SerialName("wave_id") val waveId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("display_name") val displayName: String?,
        @SerialName("departure_at") val departureAt: String
    )

    @Serializable
    private data class WaveIdRow(@SerialName("wave_id") val waveId: String)

    @Serializable
    private data class CountParams(@SerialName("wave_ids") val waveIds: List<String>)

    private val iso: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    /** Upsert so re-tapping with a new name updates the row. */
    suspend fun checkIn(waveId: String, displayName: String?, departureAt: Date) {
        val userId = SupabaseManager.ensureSession()
        val row = CheckinRow(
            waveId = waveId,
            userId = userId,
            displayName = displayName,
            departureAt = iso.format(departureAt)
        )
        SupabaseManager.client.postgrest["wave_checkins"]
            .upsert(row, onConflict = "wave_id,user_id")
    }

    suspend fun checkOut(waveId: String) {
        val userId = SupabaseManager.ensureSession()
        SupabaseManager.client.postgrest["wave_checkins"].delete {
            filter {
                eq("wave_id", waveId)
                eq("user_id", userId)
            }
        }
    }

    /** Batched counts via the wave_checkin_counts RPC. */
    suspend fun counts(waveIds: List<String>): List<WaveCheckinCount> {
        if (waveIds.isEmpty()) return emptyList()
        return SupabaseManager.client.postgrest
            .rpc("wave_checkin_counts", CountParams(waveIds))
            .decodeList<WaveCheckinCount>()
    }

    /** Wave ids the current user is checked into. */
    suspend fun myCheckins(waveIds: List<String>): Set<String> {
        if (waveIds.isEmpty()) return emptySet()
        val userId = SupabaseManager.ensureSession()
        val rows = SupabaseManager.client.postgrest["wave_checkins"]
            .select(Columns.list("wave_id")) {
                filter {
                    eq("user_id", userId)
                    isIn("wave_id", waveIds)
                }
            }
            .decodeList<WaveIdRow>()
        return rows.map { it.waveId }.toSet()
    }
}
