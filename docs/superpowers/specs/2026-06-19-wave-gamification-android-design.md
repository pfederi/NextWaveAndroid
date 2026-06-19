# Wave Gamification (Badges, Stats & Leaderboard) — Android Port

**Date:** 2026-06-19
**Status:** Design — awaiting implementation plan
**Source of truth:** iOS implementation in `pfederi/Next-Wave` (`Models/Badge.swift`,
`Models/WaveStats.swift`, `API/StatsAPI.swift`, `API/CheckinAPI.swift`,
`ViewModels/StatsStore.swift`, `Views/StatsView.swift`, `Views/LeaderboardView.swift`,
`Views/BadgeMedalView.swift`) and its design
`docs/superpowers/specs/2026-06-17-wave-gamification-design.md`.

## Summary

Port the iOS "Wave Gamification" feature to the Android app. Each user accumulates a
permanent record of the waves they have actually ridden, earns collectible achievement
badges, and can compare themselves on a global and per-station leaderboard. The feature
builds on the existing Android Wave Check-in feature and the shared Supabase backend.

## Scope

**In scope**
- Personal stats screen (total waves, badge gallery with progress, own rank).
- 28-badge catalog across categories: milestones, stations, Swiss lakes, loyalty,
  first/last ship of day, time-of-day, weekend, seasons, same-day, anniversary, social,
  weekly streaks.
- Global and per-station leaderboard.
- Extend the Android check-in to send the gamification fields so Android check-ins are
  recorded into `wave_history` (otherwise Android users never earn badges).
- Use the 18 provided illustrated badge images (`~/Downloads/badges/Variante=*.png`).

**Out of scope (explicit exclusions)**
- The 4 "verified" FoilMotion badges (`verified-distance/-longestride/-sessions/-speed`).
  Do not build, ship, or reference them.
- Any notifications (push, local, "badge unlocked!" alerts). Newly-earned badges surface
  only as an in-app celebration highlight on the stats screen.
- No SQL / migrations / backend work. The Supabase backend (tables `wave_history`,
  `user_profiles`, updated cleanup job, RPCs `user_wave_stats`, `user_station_counts`,
  `wave_leaderboard`) is already live (deployed by the iOS work). Android only consumes.

## Guiding principle

Mirror the iOS **data contracts exactly** — RPC names, JSON keys (`@SerialName`), badge
ids, thresholds, and metric mappings — so badges and the leaderboard stay byte-consistent
across platforms. Adapt only the **UI layer** to Jetpack Compose / Material3 and the
**state layer** to `ViewModel` + `StateFlow`, following the existing `CheckinStore` pattern.

---

## Part A — Extend the check-in (prerequisite)

Today `CheckinApi.checkIn` sends only `wave_id, user_id, display_name, departure_at`. The
`wave_history` table requires `station_id, lake_id, is_first_of_day, is_last_of_day` (all
`NOT NULL`); without them Android check-ins are skipped at cleanup time and earn no badges.
Mirror iOS `CheckinAPI.swift`.

1. **`CheckinContext`** (new, e.g. in `ui/checkin/`): `stationId: String`, `lakeId: String`,
   `isFirstOfDay: Boolean`, `isLastOfDay: Boolean`.
2. **`CheckinApi.checkIn(...)`**: add the 4 fields to `CheckinRow` and send them. After a
   successful check-in with a non-anonymous name, upsert `user_profiles` (`user_id`,
   `display_name`, `onConflict = "user_id"`) — the leaderboard reads this for display names.
3. **`CheckinStore.toggle(...)`**: accept a `CheckinContext` and pass it through to
   `checkIn`.
4. **`DeparturesScreen`**: compute the context at the two existing `toggle(...)` call sites
   (lines ~311 and ~369):
   - `stationId = station.id`
   - `lakeId = station.lake` (the Android `Station` already carries the lake name; fall back
     to `"unknown"` if blank, matching iOS which avoids a phantom per-station lake).
   - `isFirstOfDay` / `isLastOfDay`: computed from the loaded day's `departures` list, only
     trusted when that list actually contains this departure (else `false`, to avoid
     mis-flagging from a stale/empty list — mirrors iOS `dayLoaded`).
5. **`WaveCheckin.kt`** helpers `isFirstOfDay(time, amongDepartures)` /
   `isLastOfDay(...)`: earliest/latest departure on the same **local** calendar day
   (`Europe/Zurich`). A single departure counts as both. Mirror iOS `WaveCheckin.swift`.
6. **Optional but recommended:** when the user changes their check-in identity in Settings,
   upsert `user_profiles` with the new name (iOS `syncProfileName`) so the leaderboard name
   updates without waiting for the next check-in. If deferred, note it.

> Cross-platform invariant: `wave_id`, `station_id`, and `lake_id` must match the values iOS
> sends for the same physical wave/station/lake, or counts will not aggregate. `station_id`
> uses `station.id` on both platforms (Android already uses `station.id` as the wave-id
> station segment). `lake_id` uses the lake **name** on both.

## Part B — Consumption layer (mirrored from iOS)

| New Android file | iOS source | Notes |
|---|---|---|
| `data/models/WaveStats.kt` | `Models/WaveStats.swift` | `WaveStats`, `StationWaveCount`, `LeaderboardEntry`, all `@Serializable` with `@SerialName` exactly matching the iOS `CodingKeys`. Include a `WaveStats.empty`. |
| `data/models/Badge.kt` | `Models/Badge.swift` | `BadgeCategory` enum, `Badge` (id, title, detail, image, category, target, `metric: (WaveStats) -> Int`), `EvaluatedBadge`, `BadgeCatalog.all` (28 badges, identical ids/titles/details/targets/metrics), `BadgeEvaluator.evaluate` + `newlyEarned`. `totalLakeCount = 15`. Replace `systemImage: String` with a drawable resource id (see Part D). |
| `data/api/StatsApi.kt` | `API/StatsAPI.swift` | `object` mirroring `CheckinApi` style. `stats()` → `rpc("user_wave_stats")`; `stationCounts()` → `rpc("user_station_counts")`; `leaderboard(stationId, limit=50)` → `rpc("wave_leaderboard", {p_station_id, p_limit})`. All call `SupabaseManager.ensureSession()` first. `stats()` returns `decodeList<WaveStats>().firstOrNull() ?: WaveStats.empty`. |
| `ui/stats/StatsViewModel.kt` | `ViewModels/StatsStore.swift` | `ViewModel` exposing `StateFlow`s: `stats`, `badges` (= `BadgeEvaluator.evaluate`), `leaderboard`, `newlyEarned`, `stationCounts`, `loadFailed`. `refresh()` loads stats → evaluates badges → diffs newly-earned vs. a locally-stored "last seen" set → loads global leaderboard, then station counts independently. Persist "last seen" earned-badge ids in `SharedPreferences`. |

## Part C — UI & navigation

| New Android file | iOS source |
|---|---|
| `ui/stats/StatsScreen.kt` | `Views/StatsView.swift` — hero total, badge gallery (earned in color, locked greyed with `current/target`, tap → title+detail), own rank with link to full leaderboard, newly-earned celebration highlight, retry state on `loadFailed`. |
| `ui/stats/LeaderboardScreen.kt` | `Views/LeaderboardView.swift` — reused for global (`stationId == null`) and per-station; top-N named users; caller's own row always rendered and highlighted (`isMe`). |
| `ui/components/BadgeMedal.kt` | `Views/BadgeMedalView.swift` — single earned/locked badge cell used in the gallery grid. |

**Entry points**
- **Home top bar** (`HomeScreen`): new trophy/rosette `IconButton` → navigate to `stats`.
- **Departures top bar** (`DeparturesScreen`, `actions = { ... }`): new trophy `IconButton`
  placed left of the favorite heart → navigate to the per-station leaderboard for the
  current `station.id`.

**Navigation** (`NextWaveNavHost` / `NavRoutes`)
- `stats` → `StatsScreen`.
- `leaderboard?stationId={stationId}` (optional arg; null/absent = global) → `LeaderboardScreen`.
- From `StatsScreen`, "own rank" links to `leaderboard` (global).

## Part D — Badge images

Use the 18 illustrated PNGs in `~/Downloads/badges/Variante=*.png`. **Exclude** the 4
`verified-*.png`. Import into `app/src/main/res/` (convert to WebP and downscale to a sane
display size — the source PNGs are 200–740 KB each, far larger than needed — target a
square gallery cell). Suggested resource naming: `badge_<variant>` (e.g. `badge_milestone`).

Image → badge mapping (badges within a category/variant share one image; they differ by
title, threshold, and progress):

| Image (`Variante=`) | Badge ids |
|---|---|
| milestone | first_wave, milestone_10/25/50/100 |
| stations | stations_3/5/10 |
| lakes | lakes_2/3/all |
| loyalty | regular_10, regular_25 |
| firstship | first_ship_1, first_ship_10 |
| lastship | last_ship_1, last_ship_10 |
| timeofday | early_bird, lunch_ship, night_owl |
| weekend | weekend_warrior |
| spring / summer / autumn / winter | season_spring / _summer / _autumn / _winter |
| fourseasons | four_seasons |
| sameday | double, triple |
| anniversary | one_year |
| lonewolf | lone_wolf |
| social | crowd_surfer, trendsetter |
| streak | streak_3, streak_6 |

The Kotlin `Badge` model carries the drawable resource id (e.g. `@DrawableRes val image: Int`)
instead of the iOS `systemImage` string.

## Data flow

1. User checks in → app computes `CheckinContext` (lake, station, first/last-of-day) and
   sends it; `user_profiles` upserted with the non-anonymous name.
2. Daily cleanup job (server, already live) copies expired check-ins into `wave_history`.
3. User opens Stats → `StatsViewModel.refresh()` calls `user_wave_stats`,
   `wave_leaderboard`, `user_station_counts`.
4. `BadgeEvaluator` maps metrics → earned/locked + progress; `StatsScreen` renders the gallery.
5. Newly-earned badges (vs. local "last seen" in `SharedPreferences`) get a celebration
   highlight; the stored set is then updated.
6. Departures → leaderboard icon → `LeaderboardScreen` for that `station.id`.

## Error handling

- All RPCs are read-only. On network failure, Stats/Leaderboard show a retry state (no
  crash); the personal total falls back to "—" until reachable (`loadFailed`).
- Station counts load independently so their failure does not break badges/leaderboard.
- An anonymous caller still receives their own ranked row (`is_me = true`) and is absent
  from other users' public lists — rendered as-is from the RPC.

## Components & boundaries

| Unit | Responsibility |
|---|---|
| `CheckinContext` + extended `CheckinApi`/`CheckinStore`/`DeparturesScreen` | Capture & send gamification fields at check-in; upsert profile name. |
| `WaveStats.kt` | Decodable stats/leaderboard/station-count models. |
| `Badge.kt` (`BadgeCatalog`, `BadgeEvaluator`) | Badge definitions, thresholds, image mapping, metric→earned/progress logic. |
| `StatsApi.kt` | Read-only RPC access. |
| `StatsViewModel.kt` | Fetch + expose stats/badges/leaderboard; track "last seen". |
| `StatsScreen` / `LeaderboardScreen` / `BadgeMedal` | Compose UI. |
| Two top-bar entry-point icons + 2 nav routes | Navigation into the feature. |

## Testing

- **Kotlin unit:** `BadgeEvaluator` per-threshold (just-below / exactly-at / above), progress
  fractions, newly-earned-vs-last-seen diff. Mirror the iOS `BadgeEvaluatorTests` cases.
- **Kotlin unit:** `WaveCheckin.isFirstOfDay/isLastOfDay` — single departure counts as both;
  earliest/latest boundaries; local-day rollover in `Europe/Zurich`.
- **Serialization:** `WaveStats` / `LeaderboardEntry` / `StationWaveCount` decode from the
  RPC JSON shape (verify `@SerialName` keys).
- **Manual / instrumentation:** stats screen renders earned vs. locked correctly; leaderboard
  shows own highlighted row when outside top-N; retry state on forced network failure.

## Open implementation decisions (for the plan)

- Exact trophy/rosette icons for the two top-bar entry points (Material icon choice).
- Badge image target resolution + WebP conversion settings.
- Whether to sync `user_profiles` on Settings identity change now (Part A item 6) or defer.
- Whether the stats "last seen" set is global or namespaced per user id (matches iOS:
  simple `SharedPreferences` set; revisit only if identity reset must clear it).
