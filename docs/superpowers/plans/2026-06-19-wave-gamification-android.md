# Wave Gamification (Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the iOS "Wave Gamification" feature (collectible badges, personal stats, global + per-station leaderboard) to the Android app, consuming the already-live Supabase backend.

**Architecture:** Mirror the iOS data contracts exactly (RPC names, JSON keys, badge ids/thresholds/metrics). Adapt the UI to Jetpack Compose / Material3 and state to `ViewModel` + `StateFlow`, following the existing `CheckinStore` pattern. Extend the existing check-in to send the four gamification fields so Android check-ins are recorded into `wave_history`.

**Tech Stack:** Kotlin 1.9.22, Jetpack Compose (Material3), kotlinx.serialization, supabase-kt 2.6.0 (postgrest), JUnit4. No new dependencies.

## Global Constraints

- minSdk 26, targetSdk 35, Kotlin 1.9.22, Java 11. (from `app/build.gradle.kts`)
- No SQL / migrations / backend changes — the backend (tables `wave_history`, `user_profiles`, cleanup job, RPCs `user_wave_stats`, `user_station_counts`, `wave_leaderboard`) is already live.
- No new Gradle dependencies — everything needed (Compose, material-icons-extended, kotlinx-serialization-json, supabase postgrest) is already present.
- Mirror iOS data contracts byte-for-byte: RPC names `user_wave_stats` / `user_station_counts` / `wave_leaderboard`; leaderboard params `p_station_id`, `p_limit`; badge ids/titles/details/targets exactly as in iOS `Models/Badge.swift`; `WaveStats` JSON keys exactly as in iOS `Models/WaveStats.swift`.
- **Exclude** the 4 FoilMotion "verified" badges entirely. **No notifications** of any kind.
- `station_id` sent on check-in = `station.id`; `lake_id` = `station.lake` (the lake **name**) — must match what iOS sends.
- Time-of-day / first-last-of-day / streak semantics use local time `Europe/Zurich`.
- Test package root `com.lakeshorestudios.nextwave...` under `app/src/test/java/`, JUnit4 (`org.junit.Test`, `org.junit.Assert.*`), mirroring `WaveCheckinIdTest.kt`.
- Badge images: use the 18 illustrated PNGs in `~/Downloads/badges/Variante=*.png`. The `Badge` model carries **no** image reference (matching iOS) — the medallion composable resolves the drawable from the badge id/category, keeping the model resource-free and unit-testable.

---

### Task 1: Stats data models

**Files:**
- Create: `app/src/main/java/com/lakeshorestudios/nextwave/data/models/WaveStats.kt`
- Test: `app/src/test/java/com/lakeshorestudios/nextwave/data/models/WaveStatsSerializationTest.kt`

**Interfaces:**
- Produces:
  - `data class WaveStats(...)` with the 18 fields below + `companion object { val empty: WaveStats }`.
  - `data class StationWaveCount(val stationId: String, val waves: Int)`.
  - `data class LeaderboardEntry(val rank: Int, val displayName: String, val totalWaves: Int, val isMe: Boolean)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.lakeshorestudios.nextwave.data.models

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WaveStatsSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesWaveStatsFromRpcJsonKeys() {
        val raw = """
            {
              "total_waves": 12, "distinct_stations": 4, "distinct_lakes": 2,
              "max_waves_one_station": 6, "first_of_day_count": 1, "last_of_day_count": 0,
              "early_bird_count": 3, "lunch_count": 0, "night_owl_count": 1,
              "weekend_count": 5, "max_waves_one_day": 2, "has_anniversary": true,
              "solo_count": 1, "max_peer_count": 5, "trendsetter_count": 0,
              "seasons_ridden": ["spring","summer"], "current_streak_weeks": 2,
              "longest_streak_weeks": 3
            }
        """.trimIndent()
        val s = json.decodeFromString(WaveStats.serializer(), raw)
        assertEquals(12, s.totalWaves)
        assertEquals(2, s.distinctLakes)
        assertEquals(true, s.hasAnniversary)
        assertEquals(listOf("spring", "summer"), s.seasonsRidden)
        assertEquals(3, s.longestStreakWeeks)
    }

    @Test
    fun decodesLeaderboardEntry() {
        val raw = """{"rank":14,"display_name":"Patrick","total_waves":12,"is_me":true}"""
        val e = json.decodeFromString(LeaderboardEntry.serializer(), raw)
        assertEquals(14, e.rank)
        assertEquals("Patrick", e.displayName)
        assertEquals(true, e.isMe)
    }

    @Test
    fun emptyStatsAreAllZero() {
        assertEquals(0, WaveStats.empty.totalWaves)
        assertEquals(false, WaveStats.empty.hasAnniversary)
        assertEquals(emptyList<String>(), WaveStats.empty.seasonsRidden)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lakeshorestudios.nextwave.data.models.WaveStatsSerializationTest"`
Expected: FAIL — `WaveStats` unresolved / does not compile.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.lakeshorestudios.nextwave.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Caller's raw gamification metrics — one row from the `user_wave_stats` RPC. */
@Serializable
data class WaveStats(
    @SerialName("total_waves") val totalWaves: Int,
    @SerialName("distinct_stations") val distinctStations: Int,
    @SerialName("distinct_lakes") val distinctLakes: Int,
    @SerialName("max_waves_one_station") val maxWavesOneStation: Int,
    @SerialName("first_of_day_count") val firstOfDayCount: Int,
    @SerialName("last_of_day_count") val lastOfDayCount: Int,
    @SerialName("early_bird_count") val earlyBirdCount: Int,
    @SerialName("lunch_count") val lunchCount: Int,
    @SerialName("night_owl_count") val nightOwlCount: Int,
    @SerialName("weekend_count") val weekendCount: Int,
    @SerialName("max_waves_one_day") val maxWavesOneDay: Int,
    @SerialName("has_anniversary") val hasAnniversary: Boolean,
    @SerialName("solo_count") val soloCount: Int,
    @SerialName("max_peer_count") val maxPeerCount: Int,
    @SerialName("trendsetter_count") val trendsetterCount: Int,
    @SerialName("seasons_ridden") val seasonsRidden: List<String>,
    @SerialName("current_streak_weeks") val currentStreakWeeks: Int,
    @SerialName("longest_streak_weeks") val longestStreakWeeks: Int
) {
    companion object {
        val empty = WaveStats(
            totalWaves = 0, distinctStations = 0, distinctLakes = 0, maxWavesOneStation = 0,
            firstOfDayCount = 0, lastOfDayCount = 0, earlyBirdCount = 0, lunchCount = 0,
            nightOwlCount = 0, weekendCount = 0, maxWavesOneDay = 0, hasAnniversary = false,
            soloCount = 0, maxPeerCount = 0, trendsetterCount = 0, seasonsRidden = emptyList(),
            currentStreakWeeks = 0, longestStreakWeeks = 0
        )
    }
}

/** Caller's wave count at one station — a row from the `user_station_counts` RPC. */
@Serializable
data class StationWaveCount(
    @SerialName("station_id") val stationId: String,
    @SerialName("waves") val waves: Int
)

/** A leaderboard row from the `wave_leaderboard` RPC. */
@Serializable
data class LeaderboardEntry(
    @SerialName("rank") val rank: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("total_waves") val totalWaves: Int,
    @SerialName("is_me") val isMe: Boolean
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lakeshorestudios.nextwave.data.models.WaveStatsSerializationTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/data/models/WaveStats.kt \
        app/src/test/java/com/lakeshorestudios/nextwave/data/models/WaveStatsSerializationTest.kt
git commit -m "feat(stats): add WaveStats / StationWaveCount / LeaderboardEntry models"
```

---

### Task 2: Badge catalog + evaluator

**Files:**
- Create: `app/src/main/java/com/lakeshorestudios/nextwave/data/models/Badge.kt`
- Test: `app/src/test/java/com/lakeshorestudios/nextwave/data/models/BadgeEvaluatorTest.kt`

**Interfaces:**
- Consumes: `WaveStats` (Task 1).
- Produces:
  - `enum class BadgeCategory { MILESTONE, STATIONS, LAKES, LOYALTY, FIRST_SHIP, LAST_SHIP, TIME_OF_DAY, WEEKEND, SEASONS, SAME_DAY, ANNIVERSARY, SOCIAL, STREAK }`
  - `class Badge(val id, val title, val detail, val category, val target, val metric: (WaveStats) -> Int)` with `current(stats)` / `isEarned(stats)`.
  - `data class EvaluatedBadge(val badge: Badge, val current: Int)` with `id`, `isEarned`, `progressText`.
  - `object BadgeCatalog { const val totalLakeCount = 15; val all: List<Badge> }`.
  - `object BadgeEvaluator { fun evaluate(stats): List<EvaluatedBadge>; fun newlyEarned(stats, seenIds: Set<String>): List<Badge> }`.

- [ ] **Step 1: Write the failing test** (mirrors iOS `BadgeEvaluatorTests`)

```kotlin
package com.lakeshorestudios.nextwave.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeEvaluatorTest {

    private fun stats(
        total: Int = 0, lakes: Int = 0, early: Int = 0,
        seasons: List<String> = emptyList(), anniversary: Boolean = false,
        sameDay: Int = 0, solo: Int = 0, peer: Int = 0
    ) = WaveStats.empty.copy(
        totalWaves = total, distinctLakes = lakes, earlyBirdCount = early,
        maxWavesOneDay = sameDay, hasAnniversary = anniversary, soloCount = solo,
        maxPeerCount = peer, seasonsRidden = seasons
    )

    private fun badge(stats: WaveStats, id: String): EvaluatedBadge =
        BadgeEvaluator.evaluate(stats).first { it.id == id }

    @Test
    fun firstWaveBadgeUnlocksAtOne() {
        assertFalse(badge(stats(total = 0), "first_wave").isEarned)
        assertTrue(badge(stats(total = 1), "first_wave").isEarned)
    }

    @Test
    fun milestoneProgressTextShowsCurrentOverTarget() {
        val e = badge(stats(total = 7), "milestone_10")
        assertFalse(e.isEarned)
        assertEquals(7, e.current)
        assertEquals("7/10", e.progressText)
    }

    @Test
    fun fourSeasonsNeedsAllFour() {
        assertFalse(badge(stats(seasons = listOf("spring", "summer", "autumn")), "four_seasons").isEarned)
        assertTrue(badge(stats(seasons = listOf("spring", "summer", "autumn", "winter")), "four_seasons").isEarned)
    }

    @Test
    fun crowdSurferNeedsFivePeers() {
        assertFalse(badge(stats(peer = 4), "crowd_surfer").isEarned)
        assertTrue(badge(stats(peer = 5), "crowd_surfer").isEarned)
    }

    @Test
    fun anniversaryIsBoolean() {
        assertFalse(badge(stats(anniversary = false), "one_year").isEarned)
        assertTrue(badge(stats(anniversary = true), "one_year").isEarned)
    }

    @Test
    fun catalogHasExactlyThirtyFourBadges() {
        assertEquals(34, BadgeCatalog.all.size)
    }

    @Test
    fun newlyEarnedExcludesAlreadySeen() {
        val s = stats(total = 1, early = 1)
        val earnedIds = BadgeEvaluator.evaluate(s).filter { it.isEarned }.map { it.badge.id }.toSet()
        val seen = earnedIds - "early_bird"
        val fresh = BadgeEvaluator.newlyEarned(s, seen)
        assertEquals(listOf("early_bird"), fresh.map { it.id })
    }
}
```

> Note: this test calls `WaveStats.empty.copy(...)`. `WaveStats` is already a `data class`, so `copy` exists. No production change needed for that.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lakeshorestudios.nextwave.data.models.BadgeEvaluatorTest"`
Expected: FAIL — `BadgeCatalog` / `BadgeEvaluator` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.lakeshorestudios.nextwave.data.models

enum class BadgeCategory {
    MILESTONE, STATIONS, LAKES, LOYALTY, FIRST_SHIP, LAST_SHIP,
    TIME_OF_DAY, WEEKEND, SEASONS, SAME_DAY, ANNIVERSARY, SOCIAL, STREAK
}

/** A single achievement definition. `metric` reads the current value from the user's stats. */
class Badge(
    val id: String,
    val title: String,
    val detail: String,
    val category: BadgeCategory,
    val target: Int,
    val metric: (WaveStats) -> Int
) {
    fun current(stats: WaveStats): Int = metric(stats)
    fun isEarned(stats: WaveStats): Boolean = metric(stats) >= target
}

data class EvaluatedBadge(val badge: Badge, val current: Int) {
    val id: String get() = badge.id
    val isEarned: Boolean get() = current >= badge.target
    val progressText: String get() = if (isEarned) "Done" else "$current/${badge.target}"
}

object BadgeCatalog {
    /** Total Swiss lakes in the app data — target for the "all lakes" Swiss Explorer badge. */
    const val totalLakeCount = 15

    private fun milestone(n: Int): Badge = Badge(
        id = if (n == 1) "first_wave" else "milestone_$n",
        title = if (n == 1) "First Wave" else "$n Waves",
        detail = if (n == 1) "Your first wave" else "Ride $n waves",
        category = BadgeCategory.MILESTONE, target = n
    ) { it.totalWaves }

    private fun season(key: String, title: String): Badge = Badge(
        id = "season_$key", title = title, detail = "Ride in ${title.lowercase()}",
        category = BadgeCategory.SEASONS, target = 1
    ) { if (it.seasonsRidden.contains(key)) 1 else 0 }

    val all: List<Badge> = listOf(
        // Milestones
        milestone(1), milestone(10), milestone(25), milestone(50), milestone(100),
        // Distinct stations
        Badge("stations_3", "Explorer", "3 stations", BadgeCategory.STATIONS, 3) { it.distinctStations },
        Badge("stations_5", "Wanderer", "5 stations", BadgeCategory.STATIONS, 5) { it.distinctStations },
        Badge("stations_10", "Nomad", "10 stations", BadgeCategory.STATIONS, 10) { it.distinctStations },
        // Swiss Explorer (distinct lakes)
        Badge("lakes_2", "Two Lakes", "2 lakes", BadgeCategory.LAKES, 2) { it.distinctLakes },
        Badge("lakes_3", "Swiss Explorer", "3 lakes", BadgeCategory.LAKES, 3) { it.distinctLakes },
        Badge("lakes_all", "Swiss Champion", "All lakes", BadgeCategory.LAKES, totalLakeCount) { it.distinctLakes },
        // Loyalty (max waves at one station)
        Badge("regular_10", "Regular", "10 at one station", BadgeCategory.LOYALTY, 10) { it.maxWavesOneStation },
        Badge("regular_25", "Local Legend", "25 at one station", BadgeCategory.LOYALTY, 25) { it.maxWavesOneStation },
        // First / last ship of the day
        Badge("first_ship_1", "First Ship", "First ship of day", BadgeCategory.FIRST_SHIP, 1) { it.firstOfDayCount },
        Badge("first_ship_10", "Dawn Patrol", "First ship x10", BadgeCategory.FIRST_SHIP, 10) { it.firstOfDayCount },
        Badge("last_ship_1", "Last Ship", "Last ship of day", BadgeCategory.LAST_SHIP, 1) { it.lastOfDayCount },
        Badge("last_ship_10", "Closing Time", "Last ship x10", BadgeCategory.LAST_SHIP, 10) { it.lastOfDayCount },
        // Time of day
        Badge("early_bird", "Early Bird", "Before 08:00", BadgeCategory.TIME_OF_DAY, 1) { it.earlyBirdCount },
        Badge("lunch_ship", "Lunch Ship", "11:30-13:30", BadgeCategory.TIME_OF_DAY, 1) { it.lunchCount },
        Badge("night_owl", "Night Owl", "After 19:00", BadgeCategory.TIME_OF_DAY, 1) { it.nightOwlCount },
        // Weekend
        Badge("weekend_warrior", "Weekend Warrior", "10 on weekends", BadgeCategory.WEEKEND, 10) { it.weekendCount },
        // Seasons
        season("spring", "Spring"), season("summer", "Summer"),
        season("autumn", "Autumn"), season("winter", "Winter"),
        Badge("four_seasons", "Four Seasons", "All four seasons", BadgeCategory.SEASONS, 4) { it.seasonsRidden.size },
        // Same day
        Badge("double", "Double", "2 in one day", BadgeCategory.SAME_DAY, 2) { it.maxWavesOneDay },
        Badge("triple", "Triple", "3 in one day", BadgeCategory.SAME_DAY, 3) { it.maxWavesOneDay },
        // Anniversary
        Badge("one_year", "One Year", "A year after first", BadgeCategory.ANNIVERSARY, 1) { if (it.hasAnniversary) 1 else 0 },
        // Social
        Badge("lone_wolf", "Lone Wolf", "Ride solo", BadgeCategory.SOCIAL, 1) { it.soloCount },
        Badge("crowd_surfer", "Crowd Surfer", "5+ riders", BadgeCategory.SOCIAL, 5) { it.maxPeerCount },
        Badge("trendsetter", "Trendsetter", "First, 3+ joined", BadgeCategory.SOCIAL, 1) { it.trendsetterCount },
        // Streak
        Badge("streak_3", "On a Roll", "3 weeks streak", BadgeCategory.STREAK, 3) { it.longestStreakWeeks },
        Badge("streak_6", "Unstoppable", "6 weeks streak", BadgeCategory.STREAK, 6) { it.longestStreakWeeks },
    )
}

object BadgeEvaluator {
    fun evaluate(stats: WaveStats): List<EvaluatedBadge> =
        BadgeCatalog.all.map { EvaluatedBadge(it, it.current(stats)) }

    /** Earned-now badges whose ids are not in `seenIds`, in catalog order. */
    fun newlyEarned(stats: WaveStats, seenIds: Set<String>): List<Badge> =
        BadgeCatalog.all.filter { it.isEarned(stats) && it.id !in seenIds }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lakeshorestudios.nextwave.data.models.BadgeEvaluatorTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/data/models/Badge.kt \
        app/src/test/java/com/lakeshorestudios/nextwave/data/models/BadgeEvaluatorTest.kt
git commit -m "feat(stats): add badge catalog + evaluator (28 badges, mirrors iOS)"
```

---

### Task 3: First/last-of-day helpers on `WaveCheckin`

**Files:**
- Modify: `app/src/main/java/com/lakeshorestudios/nextwave/data/models/WaveCheckin.kt`
- Test: `app/src/test/java/com/lakeshorestudios/nextwave/data/models/WaveDayContextTest.kt`

**Interfaces:**
- Produces (added to existing `object WaveCheckin`):
  - `fun isFirstOfDay(time: Date, amongDepartures: List<Date>): Boolean`
  - `fun isLastOfDay(time: Date, amongDepartures: List<Date>): Boolean`
  - Same local calendar day = `Europe/Zurich`. A single same-day departure is both first and last.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.lakeshorestudios.nextwave.data.models

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class WaveDayContextTest {

    /** Build a Date at a given Europe/Zurich wall-clock time. */
    private fun zurich(y: Int, mo: Int, d: Int, h: Int, mi: Int): Date {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Zurich"))
        c.clear()
        c.set(y, mo - 1, d, h, mi, 0)
        return c.time
    }

    @Test
    fun earliestSameDayDepartureIsFirst() {
        val day = listOf(
            zurich(2026, 6, 18, 7, 0),
            zurich(2026, 6, 18, 12, 0),
            zurich(2026, 6, 18, 19, 30)
        )
        assertTrue(WaveCheckin.isFirstOfDay(zurich(2026, 6, 18, 7, 0), day))
        assertFalse(WaveCheckin.isFirstOfDay(zurich(2026, 6, 18, 12, 0), day))
    }

    @Test
    fun latestSameDayDepartureIsLast() {
        val day = listOf(
            zurich(2026, 6, 18, 7, 0),
            zurich(2026, 6, 18, 19, 30)
        )
        assertTrue(WaveCheckin.isLastOfDay(zurich(2026, 6, 18, 19, 30), day))
        assertFalse(WaveCheckin.isLastOfDay(zurich(2026, 6, 18, 7, 0), day))
    }

    @Test
    fun singleDepartureIsBothFirstAndLast() {
        val only = zurich(2026, 6, 18, 9, 0)
        val day = listOf(only)
        assertTrue(WaveCheckin.isFirstOfDay(only, day))
        assertTrue(WaveCheckin.isLastOfDay(only, day))
    }

    @Test
    fun otherDaysDoNotAffectTheFlag() {
        // A later departure on the NEXT day must not make today's evening sailing "not last".
        val day = listOf(
            zurich(2026, 6, 18, 19, 0),
            zurich(2026, 6, 19, 6, 0)
        )
        assertTrue(WaveCheckin.isLastOfDay(zurich(2026, 6, 18, 19, 0), day))
        assertTrue(WaveCheckin.isFirstOfDay(zurich(2026, 6, 19, 6, 0), day))
    }

    @Test
    fun departureNotInListIsNeverFlagged() {
        val day = listOf(zurich(2026, 6, 18, 7, 0))
        assertFalse(WaveCheckin.isFirstOfDay(zurich(2026, 6, 18, 8, 0), day))
        assertFalse(WaveCheckin.isLastOfDay(zurich(2026, 6, 18, 8, 0), day))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lakeshorestudios.nextwave.data.models.WaveDayContextTest"`
Expected: FAIL — `isFirstOfDay` / `isLastOfDay` unresolved.

- [ ] **Step 3: Write minimal implementation**

Add these imports at the top of `WaveCheckin.kt` (it already imports `SimpleDateFormat`, `Date`, `Locale`, `TimeZone`; add `Calendar`):

```kotlin
import java.util.Calendar
```

Add inside `object WaveCheckin { ... }` (after `makeWaveId`):

```kotlin
    /** Calendar key (year + day-of-year) in Europe/Zurich, so "the day" is the lake's local day. */
    private fun zurichDayKey(time: Date): Long {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Zurich"))
        c.time = time
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR)
    }

    private fun sameLocalDay(time: Date, among: List<Date>): List<Date> {
        val key = zurichDayKey(time)
        return among.filter { zurichDayKey(it) == key }
    }

    /** True if `time` is the earliest departure on its local (Europe/Zurich) calendar day among `among`. */
    fun isFirstOfDay(time: Date, amongDepartures: List<Date>): Boolean {
        val earliest = sameLocalDay(time, amongDepartures).minByOrNull { it.time } ?: return false
        return time.time == earliest.time
    }

    /** True if `time` is the latest departure on its local (Europe/Zurich) calendar day among `among`. */
    fun isLastOfDay(time: Date, amongDepartures: List<Date>): Boolean {
        val latest = sameLocalDay(time, amongDepartures).maxByOrNull { it.time } ?: return false
        return time.time == latest.time
    }
```

> Note: a `time` that is not present in `amongDepartures` will not equal the earliest/latest of its day, so it returns `false` — this is the desired "don't flag unknown departures" behaviour.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lakeshorestudios.nextwave.data.models.WaveDayContextTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/data/models/WaveCheckin.kt \
        app/src/test/java/com/lakeshorestudios/nextwave/data/models/WaveDayContextTest.kt
git commit -m "feat(checkin): add first/last-of-day helpers (Europe/Zurich)"
```

---

### Task 4: `CheckinContext` + extend `CheckinApi` to send gamification fields

**Files:**
- Modify: `app/src/main/java/com/lakeshorestudios/nextwave/data/api/CheckinApi.kt`
- Create: `app/src/main/java/com/lakeshorestudios/nextwave/ui/checkin/CheckinContext.kt`

**Interfaces:**
- Produces:
  - `data class CheckinContext(val stationId: String, val lakeId: String, val isFirstOfDay: Boolean, val isLastOfDay: Boolean)`
  - `CheckinApi.checkIn(waveId: String, displayName: String?, departureAt: Date, context: CheckinContext)` (signature changed — adds `context`).
  - `CheckinApi.syncProfileName(displayName: String?)` (used in Task 6).
- Consumes: `SupabaseManager.ensureSession()`.

This task has no standalone unit test (it performs network I/O against Supabase). It is verified by compilation here and by manual end-to-end check in Task 5. Mirror iOS `API/CheckinAPI.swift`.

- [ ] **Step 1: Create `CheckinContext`**

```kotlin
package com.lakeshorestudios.nextwave.ui.checkin

/** Extra gamification fields captured at check-in time and recorded into wave_history. */
data class CheckinContext(
    val stationId: String,
    val lakeId: String,
    val isFirstOfDay: Boolean,
    val isLastOfDay: Boolean
)
```

- [ ] **Step 2: Extend `CheckinRow` and `checkIn` in `CheckinApi.kt`**

Replace the `CheckinRow` data class with:

```kotlin
    @Serializable
    private data class CheckinRow(
        @SerialName("wave_id") val waveId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("display_name") val displayName: String?,
        @SerialName("departure_at") val departureAt: String,
        @SerialName("station_id") val stationId: String,
        @SerialName("lake_id") val lakeId: String,
        @SerialName("is_first_of_day") val isFirstOfDay: Boolean,
        @SerialName("is_last_of_day") val isLastOfDay: Boolean
    )
```

Add a profile row type next to `CheckinRow` (no default on `displayName`, so kotlinx always serializes it — including an explicit `null` when clearing):

```kotlin
    @Serializable
    private data class ProfileRow(
        @SerialName("user_id") val userId: String,
        @SerialName("display_name") val displayName: String?
    )
```

Replace the existing `checkIn` function with (note the new `context` parameter and the `user_profiles` upsert):

```kotlin
    /** Upsert so re-tapping with a new name updates the row. */
    suspend fun checkIn(waveId: String, displayName: String?, departureAt: Date, context: CheckinContext) {
        val userId = SupabaseManager.ensureSession()
        val row = CheckinRow(
            waveId = waveId,
            userId = userId,
            displayName = displayName,
            departureAt = iso.format(departureAt),
            stationId = context.stationId,
            lakeId = context.lakeId,
            isFirstOfDay = context.isFirstOfDay,
            isLastOfDay = context.isLastOfDay
        )
        SupabaseManager.client.postgrest["wave_checkins"]
            .upsert(row, onConflict = "wave_id,user_id")

        // Persist the latest non-anonymous name for the leaderboard.
        if (displayName != null) {
            SupabaseManager.client.postgrest["user_profiles"]
                .upsert(ProfileRow(userId = userId, displayName = displayName), onConflict = "user_id")
        }
    }

    /** Sync the leaderboard display name (or clear it with null) without a check-in. */
    suspend fun syncProfileName(displayName: String?) {
        val userId = SupabaseManager.ensureSession()
        SupabaseManager.client.postgrest["user_profiles"]
            .upsert(ProfileRow(userId = userId, displayName = displayName), onConflict = "user_id")
    }
```

Add the import for `CheckinContext` at the top of `CheckinApi.kt`:

```kotlin
import com.lakeshorestudios.nextwave.ui.checkin.CheckinContext
```

- [ ] **Step 3: Compile to verify the API change is consistent**

Run: `./gradlew compileDebugKotlin`
Expected: FAIL — `CheckinStore.kt` still calls the old `checkIn(...)` without `context`. (Fixed in Task 5.) This confirms the signature changed as intended. If any OTHER error appears, fix it before moving on.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/data/api/CheckinApi.kt \
        app/src/main/java/com/lakeshorestudios/nextwave/ui/checkin/CheckinContext.kt
git commit -m "feat(checkin): send station/lake/first-last-of-day + upsert user_profiles"
```

---

### Task 5: Wire `CheckinContext` through `CheckinStore` and `DeparturesScreen`

**Files:**
- Modify: `app/src/main/java/com/lakeshorestudios/nextwave/ui/checkin/CheckinStore.kt`
- Modify: `app/src/main/java/com/lakeshorestudios/nextwave/ui/departures/DeparturesScreen.kt`

**Interfaces:**
- Consumes: `CheckinApi.checkIn(..., context)` (Task 4), `CheckinContext` (Task 4), `WaveCheckin.isFirstOfDay/isLastOfDay` (Task 3).
- Produces: `CheckinStore.toggle(waveId: String, departureAt: Date, displayName: String?, context: CheckinContext)`.

- [ ] **Step 1: Update `CheckinStore.toggle`**

In `CheckinStore.kt`, add the import:

```kotlin
import com.lakeshorestudios.nextwave.ui.checkin.CheckinContext
```

Replace the `toggle` function with:

```kotlin
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
```

- [ ] **Step 2: Build the `CheckinContext` in `DeparturesScreen` and pass it to both `toggle` calls**

In `DeparturesScreen.kt`, inside `DeparturesScreen(...)`, the `station` and `departuresWithWaveNumbers` are already in scope where `waveIdByDeparture` is built (around line 242). Add a helper that maps a `Departure` to its `CheckinContext`. Insert this right after the `waveIdByDeparture` map is defined (after line 255):

```kotlin
                    // Full-day departure instants for first/last-of-day flags.
                    val dayDepartureTimes: List<Date> =
                        departuresWithWaveNumbers.map { it.departureDateTime }

                    fun checkinContextFor(dep: Departure): CheckinContext? {
                        val st = station ?: return null
                        val lakeId = st.lake.ifBlank { "unknown" }
                        // Only trust first/last-of-day when the day's schedule actually contains
                        // this departure; otherwise don't award the flag.
                        val dayLoaded = dayDepartureTimes.any { it.time == dep.departureDateTime.time }
                        return CheckinContext(
                            stationId = st.id,
                            lakeId = lakeId,
                            isFirstOfDay = dayLoaded &&
                                WaveCheckin.isFirstOfDay(dep.departureDateTime, dayDepartureTimes),
                            isLastOfDay = dayLoaded &&
                                WaveCheckin.isLastOfDay(dep.departureDateTime, dayDepartureTimes)
                        )
                    }
```

Update the **first** `toggle` call (in `onCheckinToggle`, around line 311) to compute and pass the context:

```kotlin
                                onCheckinToggle = {
                                    if (waveId != null) {
                                        val ctx = checkinContextFor(departure)
                                        if (ctx != null) {
                                            if (settingsViewModel.hasCheckinIdentity()) {
                                                checkinStore.toggle(
                                                    waveId = waveId,
                                                    departureAt = departure.departureDateTime,
                                                    displayName = settingsViewModel.checkinDisplayName(),
                                                    context = ctx
                                                )
                                            } else {
                                                checkinIdentityPrompt = waveId to departure.departureDateTime
                                            }
                                        }
                                    }
                                }
```

For the identity-prompt path (the `checkinIdentityPrompt?.let { ... }` block around line 363) the original `departure` object is out of scope — but the context only needs the station + the departure instant. Since the prompt stores `(waveId, departureAt: Date)`, recompute first/last-of-day from `departureAt` directly. Replace that block's `onSave` body's `toggle` call:

```kotlin
            onSave = { name, anonymous ->
                settingsViewModel.setCheckinIdentity(name, anonymous)
                val st = uiState.station
                if (st != null) {
                    val dayTimes = uiState.departures.map { it.departureDateTime }
                    val dayLoaded = dayTimes.any { it.time == departureAt.time }
                    val ctx = CheckinContext(
                        stationId = st.id,
                        lakeId = st.lake.ifBlank { "unknown" },
                        isFirstOfDay = dayLoaded && WaveCheckin.isFirstOfDay(departureAt, dayTimes),
                        isLastOfDay = dayLoaded && WaveCheckin.isLastOfDay(departureAt, dayTimes)
                    )
                    checkinStore.toggle(
                        waveId = promptWaveId,
                        departureAt = departureAt,
                        displayName = settingsViewModel.checkinDisplayName(),
                        context = ctx
                    )
                }
                checkinIdentityPrompt = null
            },
```

Add the import for `CheckinContext` at the top of `DeparturesScreen.kt`:

```kotlin
import com.lakeshorestudios.nextwave.ui.checkin.CheckinContext
```

(`Date` and `WaveCheckin` are already imported.)

- [ ] **Step 3: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: PASS (no unresolved references; the Task 4 break is now resolved).

- [ ] **Step 4: Manual end-to-end verification**

Build and install the debug app, then on a station's departures list tap a check-in:

Run: `./gradlew :app:installDebug`
Then, with the app: check into a wave, and confirm in Supabase (or via logs) that the new `wave_checkins` row carries non-null `station_id`, `lake_id`, `is_first_of_day`, `is_last_of_day`, and that a `user_profiles` row was upserted when a non-anonymous name is set. Expected: no `CheckinStore` warnings in logcat; the check-in count still updates live.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/ui/checkin/CheckinStore.kt \
        app/src/main/java/com/lakeshorestudios/nextwave/ui/departures/DeparturesScreen.kt
git commit -m "feat(checkin): pass CheckinContext from departures through to the API"
```

---

### Task 6: Sync profile name when the check-in identity changes in Settings

**Files:**
- Modify: `app/src/main/java/com/lakeshorestudios/nextwave/ui/settings/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `CheckinApi.syncProfileName(displayName: String?)` (Task 4).

This keeps the leaderboard name current when the user edits their identity without checking in. Anonymous → pushes `null` (clears the public name).

- [ ] **Step 1: Update `setCheckinIdentity` to sync the profile name**

In `SettingsViewModel.kt` add the import:

```kotlin
import com.lakeshorestudios.nextwave.data.api.CheckinApi
```

Replace `setCheckinIdentity` with:

```kotlin
    fun setCheckinIdentity(name: String, anonymous: Boolean) {
        _checkinName.value = name
        _checkinAnonymous.value = anonymous
        viewModelScope.launch {
            sharedPreferences.edit()
                .putString(KEY_CHECKIN_NAME, name)
                .putBoolean(KEY_CHECKIN_ANONYMOUS, anonymous)
                .apply()
            // Push the resolved display name (or null when anonymous/blank) to the leaderboard.
            try {
                CheckinApi.syncProfileName(checkinDisplayName())
            } catch (e: Exception) {
                // Non-fatal: the name will also be upserted on the next check-in.
            }
        }
    }
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/ui/settings/SettingsViewModel.kt
git commit -m "feat(stats): sync leaderboard profile name on identity change"
```

---

### Task 7: `StatsApi` — read-only RPC access

**Files:**
- Create: `app/src/main/java/com/lakeshorestudios/nextwave/data/api/StatsApi.kt`
- Test: `app/src/test/java/com/lakeshorestudios/nextwave/data/api/StatsApiParamsTest.kt`

**Interfaces:**
- Consumes: `WaveStats`, `StationWaveCount`, `LeaderboardEntry` (Task 1); `SupabaseManager` (existing).
- Produces:
  - `suspend fun StatsApi.stats(): WaveStats`
  - `suspend fun StatsApi.stationCounts(): List<StationWaveCount>`
  - `suspend fun StatsApi.leaderboard(stationId: String?, limit: Int = 50): List<LeaderboardEntry>`

The RPC calls hit the network, so the unit test only covers the params payload serialization (the part with logic). Mirror iOS `API/StatsAPI.swift`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.lakeshorestudios.nextwave.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsApiParamsTest {

    @Test
    fun leaderboardParamsSerializeWithRpcKeys() {
        val json = Json { encodeDefaults = true }
        val params = StatsApi.LeaderboardParams(stationId = "Thalwil_8503671", limit = 50)
        val encoded = json.encodeToString(StatsApi.LeaderboardParams.serializer(), params)
        assertEquals("""{"p_station_id":"Thalwil_8503671","p_limit":50}""", encoded)
    }

    @Test
    fun leaderboardParamsAllowNullStation() {
        val json = Json { encodeDefaults = true }
        val params = StatsApi.LeaderboardParams(stationId = null, limit = 25)
        val encoded = json.encodeToString(StatsApi.LeaderboardParams.serializer(), params)
        assertEquals("""{"p_station_id":null,"p_limit":25}""", encoded)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lakeshorestudios.nextwave.data.api.StatsApiParamsTest"`
Expected: FAIL — `StatsApi` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.lakeshorestudios.nextwave.data.api

import com.lakeshorestudios.nextwave.data.models.LeaderboardEntry
import com.lakeshorestudios.nextwave.data.models.StationWaveCount
import com.lakeshorestudios.nextwave.data.models.WaveStats
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Read-only gamification RPCs. Mirrors iOS API/StatsAPI.swift. */
object StatsApi {

    @Serializable
    data class LeaderboardParams(
        @SerialName("p_station_id") val stationId: String?,
        @SerialName("p_limit") val limit: Int
    )

    /** Caller's raw metrics — `user_wave_stats()` returns a single row. */
    suspend fun stats(): WaveStats {
        SupabaseManager.ensureSession()
        return SupabaseManager.client.postgrest
            .rpc("user_wave_stats")
            .decodeList<WaveStats>()
            .firstOrNull() ?: WaveStats.empty
    }

    /** Caller's wave count per station, most-ridden first. */
    suspend fun stationCounts(): List<StationWaveCount> {
        SupabaseManager.ensureSession()
        return SupabaseManager.client.postgrest
            .rpc("user_station_counts")
            .decodeList<StationWaveCount>()
    }

    /** Global (stationId == null) or per-station leaderboard: top `limit` named users + own row. */
    suspend fun leaderboard(stationId: String?, limit: Int = 50): List<LeaderboardEntry> {
        SupabaseManager.ensureSession()
        return SupabaseManager.client.postgrest
            .rpc("wave_leaderboard", LeaderboardParams(stationId, limit))
            .decodeList<LeaderboardEntry>()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lakeshorestudios.nextwave.data.api.StatsApiParamsTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/data/api/StatsApi.kt \
        app/src/test/java/com/lakeshorestudios/nextwave/data/api/StatsApiParamsTest.kt
git commit -m "feat(stats): add StatsApi (user_wave_stats / station_counts / leaderboard)"
```

---

### Task 8: Import badge images into resources

**Files:**
- Create: `app/src/main/res/drawable-nodpi/badge_*.png` (18 files)

The source PNGs (200–740 KB each) are downscaled to ≤512 px for the medallion (rendered at ~120 dp). `drawable-nodpi` prevents density scaling. Use macOS `sips`. Exclude the 4 `verified-*.png`.

- [ ] **Step 1: Convert & copy the 18 badge images**

Run (from anywhere):

```bash
DEST="app/src/main/res/drawable-nodpi"
mkdir -p "$DEST"
for f in "$HOME/Downloads/badges/Variante="*.png; do
  base="$(basename "$f")"          # e.g. "Variante=milestone.png"
  v="${base#Variante=}"; v="${v%.png}"   # e.g. "milestone"
  sips -Z 512 -s format png "$f" --out "$DEST/badge_${v}.png" >/dev/null
done
ls "$DEST"
```

Expected `ls` output (18 files): `badge_anniversary.png badge_autumn.png badge_firstship.png badge_fourseasons.png badge_lakes.png badge_lastship.png badge_lonewolf.png badge_loyalty.png badge_milestone.png badge_sameday.png badge_social.png badge_spring.png badge_stations.png badge_streak.png badge_summer.png badge_timeofday.png badge_weekend.png badge_winter.png`

- [ ] **Step 2: Verify resource names are valid and the project still builds**

Run: `./gradlew :app:processDebugResources`
Expected: PASS (no invalid-resource-name errors). Resource names are all lowercase `badge_<variant>` — valid.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable-nodpi/badge_*.png
git commit -m "feat(stats): add 18 badge illustration drawables (verified set excluded)"
```

---

### Task 9: `BadgeMedal` composable — circular medallion

**Files:**
- Create: `app/src/main/java/com/lakeshorestudios/nextwave/ui/components/BadgeMedal.kt`

**Interfaces:**
- Consumes: `Badge`, `BadgeCategory` (Task 2); `R.drawable.badge_*` (Task 8).
- Produces: `@Composable fun BadgeMedal(badge: Badge, isEarned: Boolean, size: Dp = 96.dp, modifier: Modifier = Modifier)`.

Verified via a `@Preview` and a build (no unit test for a pure visual). Mirrors iOS `Views/BadgeMedalView.swift`.

- [ ] **Step 1: Write the composable**

```kotlin
package com.lakeshorestudios.nextwave.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.lakeshorestudios.nextwave.R
import com.lakeshorestudios.nextwave.data.models.Badge
import com.lakeshorestudios.nextwave.data.models.BadgeCatalog
import com.lakeshorestudios.nextwave.data.models.BadgeCategory

private val Cream = Color(0xFFF3E6C9)

/** Untappd-style circular badge medallion. Renders only the graphic — no title/caption. */
@Composable
fun BadgeMedal(
    badge: Badge,
    isEarned: Boolean,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = size * 0.04f, shape = CircleShape, clip = false),
        contentAlignment = Alignment.Center
    ) {
        // Category ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ringColor(badge.category))
        )
        // Cream rim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.045f)
                .clip(CircleShape)
                .background(Cream)
        )
        // Illustration (greyscale when locked)
        Image(
            painter = painterResource(badgeDrawable(badge)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (isEarned) null else grayscale,
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.075f)
                .clip(CircleShape)
        )
        // Locked overlay
        if (!isEarned) {
            Box(
                modifier = Modifier
                    .size(size * 0.44f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(red = 0.23f, green = 0.23f, blue = 0.23f),
                    modifier = Modifier.size(size * 0.22f)
                )
            }
        }
    }
}

private fun ringColor(category: BadgeCategory): Color = when (category) {
    BadgeCategory.MILESTONE -> Color(0xFF1E88E5)
    BadgeCategory.STATIONS -> Color(0xFF26A69A)
    BadgeCategory.LAKES -> Color(0xFF43A047)
    BadgeCategory.LOYALTY -> Color(0xFFFB8C00)
    BadgeCategory.FIRST_SHIP -> Color(0xFFFF7043)
    BadgeCategory.LAST_SHIP -> Color(0xFF8E24AA)
    BadgeCategory.TIME_OF_DAY -> Color(0xFF5C6BC0)
    BadgeCategory.WEEKEND -> Color(0xFFEC407A)
    BadgeCategory.SEASONS -> Color(0xFF00897B)
    BadgeCategory.SAME_DAY -> Color(0xFF00ACC1)
    BadgeCategory.ANNIVERSARY -> Color(0xFFF9A825)
    BadgeCategory.SOCIAL -> Color(0xFFE53935)
    BadgeCategory.STREAK -> Color(0xFFF4511E)
}

@DrawableRes
private fun badgeDrawable(badge: Badge): Int = when (badge.id) {
    "season_spring" -> R.drawable.badge_spring
    "season_summer" -> R.drawable.badge_summer
    "season_autumn" -> R.drawable.badge_autumn
    "season_winter" -> R.drawable.badge_winter
    "four_seasons" -> R.drawable.badge_fourseasons
    "lone_wolf" -> R.drawable.badge_lonewolf
    else -> when (badge.category) {
        BadgeCategory.MILESTONE -> R.drawable.badge_milestone
        BadgeCategory.STATIONS -> R.drawable.badge_stations
        BadgeCategory.LAKES -> R.drawable.badge_lakes
        BadgeCategory.LOYALTY -> R.drawable.badge_loyalty
        BadgeCategory.FIRST_SHIP -> R.drawable.badge_firstship
        BadgeCategory.LAST_SHIP -> R.drawable.badge_lastship
        BadgeCategory.TIME_OF_DAY -> R.drawable.badge_timeofday
        BadgeCategory.WEEKEND -> R.drawable.badge_weekend
        BadgeCategory.SEASONS -> R.drawable.badge_fourseasons
        BadgeCategory.SAME_DAY -> R.drawable.badge_sameday
        BadgeCategory.ANNIVERSARY -> R.drawable.badge_anniversary
        BadgeCategory.SOCIAL -> R.drawable.badge_social
        BadgeCategory.STREAK -> R.drawable.badge_streak
    }
}

@Preview
@Composable
private fun BadgeMedalEarnedPreview() {
    BadgeMedal(badge = BadgeCatalog.all.first(), isEarned = true, size = 120.dp)
}

@Preview
@Composable
private fun BadgeMedalLockedPreview() {
    BadgeMedal(badge = BadgeCatalog.all.first { it.id == "milestone_100" }, isEarned = false, size = 120.dp)
}
```

- [ ] **Step 2: Build to verify it compiles and resources resolve**

Run: `./gradlew compileDebugKotlin`
Expected: PASS — all `R.drawable.badge_*` resolve (Task 8 added them).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/ui/components/BadgeMedal.kt
git commit -m "feat(stats): circular badge medallion composable (mirrors iOS)"
```

---

### Task 10: `StatsViewModel`

**Files:**
- Create: `app/src/main/java/com/lakeshorestudios/nextwave/ui/stats/StatsViewModel.kt`

**Interfaces:**
- Consumes: `StatsApi` (Task 7); `BadgeEvaluator`, `EvaluatedBadge`, `Badge`, `WaveStats` (Tasks 1–2).
- Produces a `class StatsViewModel(application) : AndroidViewModel` exposing `StateFlow`s:
  - `stats: StateFlow<WaveStats?>`
  - `badges: StateFlow<List<EvaluatedBadge>>`
  - `leaderboard: StateFlow<List<LeaderboardEntry>>`
  - `newlyEarned: StateFlow<List<Badge>>`
  - `stationCounts: StateFlow<List<StationWaveCount>>`
  - `loadFailed: StateFlow<Boolean>`
  - `fun refresh()` and `fun loadLeaderboard(stationId: String?)`.

Mirrors iOS `ViewModels/StatsStore.swift`; "last seen" badge ids persist in the existing `app_settings` SharedPreferences under key `seen_badge_ids`.

- [ ] **Step 1: Write the ViewModel**

```kotlin
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
```

- [ ] **Step 2: Build**

Run: `./gradlew compileDebugKotlin`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/ui/stats/StatsViewModel.kt
git commit -m "feat(stats): StatsViewModel (stats/badges/leaderboard + last-seen diff)"
```

---

### Task 11: `StatsScreen` (personal stats + badge gallery)

**Files:**
- Create: `app/src/main/java/com/lakeshorestudios/nextwave/ui/stats/StatsScreen.kt`

**Interfaces:**
- Consumes: `StatsViewModel` (Task 10); `BadgeMedal` (Task 9); `EvaluatedBadge`, `LeaderboardEntry` (Tasks 1–2).
- Produces: `@Composable fun StatsScreen(onBackClick: () -> Unit, onLeaderboardClick: () -> Unit, viewModel: StatsViewModel = viewModel())`.

Mirrors iOS `Views/StatsView.swift`: newly-earned banner, hero total, a "Leaderboard" row showing the caller's rank, then Earned and Locked badge sections (title + detail under each medallion). Uses the app's header colors (as `DeparturesScreen` does).

- [ ] **Step 1: Write the screen**

```kotlin
package com.lakeshorestudios.nextwave.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lakeshorestudios.nextwave.data.models.EvaluatedBadge
import com.lakeshorestudios.nextwave.ui.components.BadgeMedal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val newlyEarned by viewModel.newlyEarned.collectAsState()

    val headerBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerBackground
    val headerTextColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerText
    val mainBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.mainBackground

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Badges", color = headerTextColor) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = headerTextColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerBackgroundColor,
                    titleContentColor = headerTextColor,
                    navigationIconContentColor = headerTextColor
                )
            )
        },
        containerColor = mainBackgroundColor
    ) { padding ->
        val earned = badges.filter { it.isEarned }
        val locked = badges.filter { !it.isEarned }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Newly-earned celebration banner
            if (newlyEarned.isNotEmpty()) {
                fullRow {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 New badge${if (newlyEarned.size > 1) "s" else ""}!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = newlyEarned.joinToString(", ") { it.title },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Hero total + leaderboard row
            fullRow {
                Column(modifier = Modifier.fillMaxWidth().padding(top = if (newlyEarned.isEmpty()) 16.dp else 0.dp)) {
                    val total = stats?.totalWaves ?: 0
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$total", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (total == 1) "wave ridden" else "waves ridden",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLeaderboardClick() }
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Leaderboard", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        val me = leaderboard.firstOrNull { it.isMe }
                        if (me != null) {
                            Text(
                                text = if (me.totalWaves > 0) "You — #${me.rank}" else "Not ranked yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (earned.isNotEmpty()) {
                fullRow { SectionHeader("Earned (${earned.size})") }
                items(earned) { BadgeCell(it) }
            }
            if (locked.isNotEmpty()) {
                fullRow { SectionHeader("Locked (${locked.size})") }
                items(locked) { BadgeCell(it) }
            }

            fullRow { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** A grid item that spans both columns. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullRow(
    content: @Composable () -> Unit
) {
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BadgeCell(item: EvaluatedBadge) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BadgeMedal(badge = item.badge, isEarned = item.isEarned, size = 120.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = item.badge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (item.isEarned) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (item.isEarned) item.badge.detail else item.progressText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

> Import note: the `LazyGridScope` / `GridItemSpan` references in `fullRow` are fully-qualified inline, so no extra import is required beyond what is listed; if the IDE prefers explicit imports, add `import androidx.compose.foundation.lazy.grid.LazyGridScope` and `import androidx.compose.foundation.lazy.grid.GridItemSpan`.

- [ ] **Step 2: Build**

Run: `./gradlew compileDebugKotlin`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/ui/stats/StatsScreen.kt
git commit -m "feat(stats): StatsScreen with hero total, leaderboard row, badge gallery"
```

---

### Task 12: `LeaderboardScreen` (global / per-station)

**Files:**
- Create: `app/src/main/java/com/lakeshorestudios/nextwave/ui/stats/LeaderboardScreen.kt`

**Interfaces:**
- Consumes: `StatsViewModel.loadLeaderboard(stationId)` + `leaderboard` / `loadFailed` (Task 10).
- Produces: `@Composable fun LeaderboardScreen(stationId: String?, title: String, onBackClick: () -> Unit, viewModel: StatsViewModel = viewModel())`.

Mirrors iOS `Views/LeaderboardView.swift`: ranked list, caller's own row highlighted, empty + error states.

- [ ] **Step 1: Write the screen**

```kotlin
package com.lakeshorestudios.nextwave.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lakeshorestudios.nextwave.data.models.LeaderboardEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    stationId: String?,
    title: String,
    onBackClick: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val loadFailed by viewModel.loadFailed.collectAsState()

    val headerBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerBackground
    val headerTextColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerText
    val mainBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.mainBackground

    LaunchedEffect(stationId) { viewModel.loadLeaderboard(stationId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = headerTextColor) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = headerTextColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerBackgroundColor,
                    titleContentColor = headerTextColor,
                    navigationIconContentColor = headerTextColor
                )
            )
        },
        containerColor = mainBackgroundColor
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loadFailed -> Text(
                    "Couldn't load the leaderboard. Pull to retry.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                leaderboard.isEmpty() -> Text(
                    "No rides recorded yet — be the first! 🌊",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    items(leaderboard.sortedBy { it.rank }) { entry -> LeaderboardRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val highlight = if (entry.isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlight)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${entry.rank}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = entry.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.Water, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text("${entry.totalWaves}", style = MaterialTheme.typography.titleMedium)
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew compileDebugKotlin`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/ui/stats/LeaderboardScreen.kt
git commit -m "feat(stats): LeaderboardScreen (global/per-station, own row highlighted)"
```

---

### Task 13: Navigation routes + entry-point icons

**Files:**
- Modify: `app/src/main/java/com/lakeshorestudios/nextwave/ui/navigation/NextWaveNavHost.kt`
- Modify: `app/src/main/java/com/lakeshorestudios/nextwave/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/lakeshorestudios/nextwave/ui/departures/DeparturesScreen.kt`

**Interfaces:**
- Consumes: `StatsScreen` (Task 11), `LeaderboardScreen` (Task 12).
- Produces: routes `stats` and `leaderboard?stationId={stationId}`; a `HomeScreen` `onBadgesClick` callback; a leaderboard icon in the `DeparturesScreen` top bar.

- [ ] **Step 1: Add routes to `NextWaveNavHost.kt`**

In `object NavRoutes`, add:

```kotlin
    const val STATS_SCREEN = "stats"
    const val LEADERBOARD_SCREEN = "leaderboard?stationId={stationId}"

    fun leaderboardRoute(stationId: String? = null): String =
        if (stationId == null) "leaderboard" else "leaderboard?stationId=$stationId"
```

Add imports at the top:

```kotlin
import com.lakeshorestudios.nextwave.ui.stats.StatsScreen
import com.lakeshorestudios.nextwave.ui.stats.LeaderboardScreen
```

In the `HomeScreen(...)` composable call inside the `HOME_SCREEN` destination, add the badges navigation callback:

```kotlin
        composable(NavRoutes.HOME_SCREEN) {
            HomeScreen(
                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS_SCREEN) },
                onBadgesClick = { navController.navigate(NavRoutes.STATS_SCREEN) },
                onStationSelected = { station ->
                    navController.navigate(NavRoutes.departuresRoute(station.id))
                }
            )
        }
```

(Also add the same `onBadgesClick` to the `STATION_SELECT_SCREEN` `HomeScreen(...)` usage so it still compiles — pass `onBadgesClick = { navController.navigate(NavRoutes.STATS_SCREEN) }`.)

Add the two new destinations inside the `NavHost { ... }` block:

```kotlin
        composable(NavRoutes.STATS_SCREEN) {
            StatsScreen(
                onBackClick = { navController.popBackStack() },
                onLeaderboardClick = { navController.navigate(NavRoutes.leaderboardRoute(null)) }
            )
        }

        composable(
            route = NavRoutes.LEADERBOARD_SCREEN,
            arguments = listOf(
                androidx.navigation.navArgument("stationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val stationId = backStackEntry.arguments?.getString("stationId")
            LeaderboardScreen(
                stationId = stationId,
                title = "Leaderboard",
                onBackClick = { navController.popBackStack() }
            )
        }
```

- [ ] **Step 2: Add the trophy icon to `HomeScreen`**

In `HomeScreen.kt`, change the signature to accept the new callback:

```kotlin
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onBadgesClick: () -> Unit,
    onStationSelected: (Station) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
```

Add the import:

```kotlin
import androidx.compose.material.icons.outlined.EmojiEvents
```

In the `TopAppBar` `actions = { ... }`, add a trophy button before the existing rules button:

```kotlin
                actions = {
                    IconButton(onClick = onBadgesClick) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = "My Badges",
                            tint = headerTextColor
                        )
                    }
                    IconButton(onClick = { showNavigationRules = true }) {
                        Icon(
                            imageVector = Lucide.ShieldAlert,
                            contentDescription = "Wakethieving Rules",
                            tint = Color(0xFFE65100)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = headerTextColor
                        )
                    }
                },
```

- [ ] **Step 3: Add the per-station leaderboard icon to `DeparturesScreen`**

`DeparturesScreen` needs to navigate. Add an `onLeaderboardClick: (stationId: String) -> Unit` parameter to `DeparturesScreen(...)` and wire it from the nav host.

In `NextWaveNavHost.kt`, update the `DEPARTURES_SCREEN` destination's `DeparturesScreen(...)` call:

```kotlin
            DeparturesScreen(
                stationId = stationId,
                onBackClick = { navController.popBackStack() },
                onLeaderboardClick = { sid -> navController.navigate(NavRoutes.leaderboardRoute(sid)) }
            )
```

In `DeparturesScreen.kt`, update the signature:

```kotlin
fun DeparturesScreen(
    @Suppress("UNUSED_PARAMETER") stationId: String,
    onBackClick: () -> Unit,
    onLeaderboardClick: (String) -> Unit,
    viewModel: DeparturesViewModel = viewModel(),
    checkinStore: CheckinStore = viewModel(),
    settingsViewModel: com.lakeshorestudios.nextwave.ui.settings.SettingsViewModel = viewModel()
) {
```

Add the import:

```kotlin
import androidx.compose.material.icons.outlined.EmojiEvents
```

In the `TopAppBar` `actions = { ... }`, add a leaderboard button **before** the favorite button:

```kotlin
                actions = {
                    // Per-station leaderboard
                    IconButton(
                        onClick = { uiState.station?.let { onLeaderboardClick(it.id) } }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = "Station leaderboard",
                            tint = headerTextColor
                        )
                    }
                    // Favorite icon
                    IconButton(
                        onClick = { uiState.station?.let { station -> viewModel.toggleFavorite(station) } }
                    ) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = headerTextColor
                        )
                    }
                },
```

- [ ] **Step 4: Build the whole app**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual smoke test**

Run: `./gradlew :app:installDebug`
Then verify: Home top-bar trophy → Stats screen loads (hero total, badge gallery with circular medallions, leaderboard row). Tapping the leaderboard row → global leaderboard. On a station's departures screen, the trophy in the top bar → that station's leaderboard. Locked badges appear greyscale with a lock; earned ones in color.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lakeshorestudios/nextwave/ui/navigation/NextWaveNavHost.kt \
        app/src/main/java/com/lakeshorestudios/nextwave/ui/home/HomeScreen.kt \
        app/src/main/java/com/lakeshorestudios/nextwave/ui/departures/DeparturesScreen.kt
git commit -m "feat(stats): wire navigation + trophy entry points (home + station)"
```

---

## Final verification

- [ ] Run the full unit-test suite: `./gradlew testDebugUnitTest` — expect all green (WaveStats, BadgeEvaluator, WaveDayContext, StatsApiParams + existing tests).
- [ ] Run a release-config compile to catch R8/resource issues: `./gradlew assembleDebug`.
- [ ] Manual: full walkthrough per Task 13 Step 5 on a device/emulator with a real (anonymous) session.

## Notes / deferred

- "My Stations" (the iOS `MyStationsView` per-station personal counts list) is intentionally **not** ported in this plan — the `user_station_counts` data is already fetched by `StatsViewModel.stationCounts` and can drive a future screen. Add it as a follow-up if desired; it is not required for the core badges/stats/leaderboard feature.
- Badge images are shipped as ≤512 px PNGs. If APK size matters, convert them to WebP via Android Studio ("Convert to WebP…") in a follow-up — names and references stay identical.
