package com.lakeshorestudios.nextwave.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lakeshorestudios.nextwave.R
import com.lakeshorestudios.nextwave.data.models.Badge
import com.lakeshorestudios.nextwave.data.models.BadgeCatalog
import com.lakeshorestudios.nextwave.data.models.BadgeCategory

private val Cream = Color(0xFFF3E6C9)
private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Untappd-style circular badge medallion. Renders only the graphic — no title/caption. */
@Composable
fun BadgeMedal(
    badge: Badge,
    isEarned: Boolean,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
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
        AsyncImage(
            model = badgeDrawable(badge),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (isEarned) null else GrayscaleFilter,
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
