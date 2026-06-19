package com.lakeshorestudios.nextwave.ui.checkin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakeshorestudios.nextwave.data.api.CheckinApi
import com.lakeshorestudios.nextwave.data.api.SupabaseManager
import com.lakeshorestudios.nextwave.ui.checkin.CheckinContext
import com.lakeshorestudios.nextwave.data.models.WaveCheckinCount
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class CheckinStore : ViewModel() {

    private val _counts = MutableStateFlow<Map<String, WaveCheckinCount>>(emptyMap())
    val counts: StateFlow<Map<String, WaveCheckinCount>> = _counts.asStateFlow()

    private val _mine = MutableStateFlow<Set<String>>(emptySet())
    val mine: StateFlow<Set<String>> = _mine.asStateFlow()

    @Volatile private var visibleWaveIds: List<String> = emptyList()
    @Volatile private var didSubscribe = false
    @Volatile private var realtimeJob: Job? = null
    private var realtimeChannel: RealtimeChannel? = null

    // Dedicated scope for the single teardown launch in onCleared(). Not cancelled after launch
    // because cancelling it would abort the in-flight removeChannel before it completes. The scope
    // is bounded: it is created once per ViewModel instance and GC'd with this ViewModel.
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Load counts + my-state for the visible waves and ensure a realtime subscription. */
    fun refresh(waveIds: List<String>) {
        visibleWaveIds = waveIds
        viewModelScope.launch {
            reloadCounts()
            try {
                _mine.value = CheckinApi.myCheckins(waveIds)
            } catch (e: Exception) {
                Log.w("CheckinStore", "myCheckins failed: ${e.message}")
            }
            ensureSubscribed()
        }
    }

    private suspend fun reloadCounts() {
        try {
            val fetched = CheckinApi.counts(visibleWaveIds)
            _counts.value = fetched.associateBy { it.waveId }
        } catch (e: Exception) {
            Log.w("CheckinStore", "counts failed: ${e.message}")
        }
    }

    private fun ensureSubscribed() {
        if (didSubscribe) return
        didSubscribe = true
        realtimeJob = viewModelScope.launch {
            try {
                SupabaseManager.ensureSession()
                val channel = SupabaseManager.client.channel("wave_checkins_live")
                realtimeChannel = channel
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "wave_checkins"
                }
                channel.subscribe()
                changes.collect {
                    reloadCounts()
                    try {
                        _mine.value = CheckinApi.myCheckins(visibleWaveIds)
                    } catch (e: Exception) {
                        Log.w("CheckinStore", "myCheckins (realtime) failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("CheckinStore", "realtime subscribe failed: ${e.message}")
                didSubscribe = false  // allow a later retry
            }
        }
    }

    fun toggle(waveId: String, departureAt: Date, displayName: String?, context: CheckinContext) {
        viewModelScope.launch {
            try {
                if (_mine.value.contains(waveId)) {
                    CheckinApi.checkOut(waveId)
                } else {
                    CheckinApi.checkIn(waveId, displayName, departureAt, context)
                }
                reloadCounts()
                _mine.value = CheckinApi.myCheckins(visibleWaveIds)
            } catch (e: Exception) {
                Log.w("CheckinStore", "toggle failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        realtimeJob?.cancel()
        val channel = realtimeChannel
        if (channel != null) {
            cleanupScope.launch {
                runCatching { SupabaseManager.client.realtime.removeChannel(channel) }
            }
        }
        super.onCleared()
    }
}
