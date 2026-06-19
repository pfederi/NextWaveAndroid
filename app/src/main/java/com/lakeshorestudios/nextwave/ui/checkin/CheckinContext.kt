package com.lakeshorestudios.nextwave.ui.checkin

/** Extra gamification fields captured at check-in time and recorded into wave_history. */
data class CheckinContext(
    val stationId: String,
    val lakeId: String,
    val isFirstOfDay: Boolean,
    val isLastOfDay: Boolean
)
