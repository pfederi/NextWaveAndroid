package com.lakeshorestudios.nextwave.ui.stats

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lakeshorestudios.nextwave.data.api.StatsApi
import com.lakeshorestudios.nextwave.data.models.Badge
import com.lakeshorestudios.nextwave.data.models.BadgeEvaluator
import com.lakeshorestudios.nextwave.data.models.EvaluatedBadge
import com.lakeshorestudios.nextwave.data.models.LeaderboardEntry
import com.lakeshorestudios.nextwave.data.models.StationWaveCount
import com.lakeshorestudios.nextwave.data.models.WaveStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _stats = MutableStateFlow<WaveStats?>(null)
    val stats: StateFlow<WaveStats?> = _stats.asStateFlow()

    private val _badges = MutableStateFlow(BadgeEvaluator.evaluate(WaveStats.empty))
    val badges: StateFlow<List<EvaluatedBadge>> = _badges.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _newlyEarned = MutableStateFlow<List<Badge>>(emptyList())
    val newlyEarned: StateFlow<List<Badge>> = _newlyEarned.asStateFlow()

    private val _stationCounts = MutableStateFlow<List<StationWaveCount>>(emptyList())
    val stationCounts: StateFlow<List<StationWaveCount>> = _stationCounts.asStateFlow()

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    /** Load global stats + badges + global leaderboard + station counts. */
    fun refresh() {
        viewModelScope.launch {
            _loadFailed.value = false
            try {
                val fetched = StatsApi.stats()
                _stats.value = fetched
                val evaluated = BadgeEvaluator.evaluate(fetched)
                _badges.value = evaluated
                val seen = seenBadgeIds()
                _newlyEarned.value = BadgeEvaluator.newlyEarned(fetched, seen)
                val earnedNow = evaluated.filter { it.isEarned }.map { it.badge.id }.toSet()
                saveSeenBadgeIds(seen + earnedNow)
                _leaderboard.value = StatsApi.leaderboard(stationId = null)
            } catch (e: Exception) {
                Log.w("StatsViewModel", "stats refresh failed: ${e.message}")
                _loadFailed.value = true
            }
            // Independent: a failure here must not break badges/leaderboard.
            try {
                _stationCounts.value = StatsApi.stationCounts()
            } catch (e: Exception) {
                Log.w("StatsViewModel", "station counts failed: ${e.message}")
            }
        }
    }

    /** Load a leaderboard scoped to one station (null == global). */
    fun loadLeaderboard(stationId: String?) {
        viewModelScope.launch {
            _loadFailed.value = false
            try {
                _leaderboard.value = StatsApi.leaderboard(stationId)
            } catch (e: Exception) {
                Log.w("StatsViewModel", "leaderboard load failed: ${e.message}")
                _loadFailed.value = true
            }
        }
    }

    private fun seenBadgeIds(): Set<String> =
        prefs.getStringSet(KEY_SEEN_BADGES, emptySet())?.toSet() ?: emptySet()

    private fun saveSeenBadgeIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_SEEN_BADGES, ids).apply()
    }

    private companion object {
        const val KEY_SEEN_BADGES = "seen_badge_ids"
    }
}
