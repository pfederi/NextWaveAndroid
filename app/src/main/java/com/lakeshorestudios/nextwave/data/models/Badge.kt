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
