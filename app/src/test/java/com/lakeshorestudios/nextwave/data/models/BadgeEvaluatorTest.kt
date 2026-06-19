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
