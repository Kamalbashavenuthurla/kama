# KAMA – Discipline & Progress Tracker

Offline-first native Android habit tracker (Kotlin, Jetpack Compose, Material 3, Room, MVVM).
Default tracker name is **Kama** and can be changed in Profile/Settings.

> **Status: foundation (build steps 1–6 core).** See "What is implemented" and "Roadmap".
> This code was written without access to the Android SDK or Gradle repositories, so it has
> **not been compiled or run yet**. Expect to fix a few compile errors on first build.

## Build & run

Requirements: Android Studio Ladybug (2024.2) or newer, JDK 17 (bundled with Android Studio).

1. Open the `Kama` folder in Android Studio and let Gradle sync (first sync downloads dependencies).
2. Run the unit tests: `./gradlew test` (streak logic: `StreakEngineTest`).
3. Install on the phone (USB debugging on): `./gradlew installDebug`, or press Run.
4. Release APK: `./gradlew assembleRelease` -> `app/build/outputs/apk/release/app-release.apk`.
   - Without a `keystore.properties` the release build is signed with the debug key (installable, but not for Play Store).
   - For your own key, create `keystore.properties` in the project root:
     ```
     storeFile=my-release.jks
     storePassword=...
     keyAlias=...
     keyPassword=...
     ```
   - Android Studio alternative: Build > Generate Signed App Bundle / APK.

If `./gradlew` is missing, run `gradle wrapper --gradle-version 8.9` once, or just use Android Studio.

## What is implemented

| Area | State |
|---|---|
| Project, Gradle version catalog, dark/light Material 3 theme (orange accent, green done, red missed) | done |
| Room DB v1: habits, pauses, completions (unique per habit+day), evidence, goals, milestones, goal-habit links, reminders, achievements, settings; exported schemas; migration scaffold; no destructive fallback | done |
| Onboarding (name, tracker name, first habit) | done |
| Home: greeting, date, overall streak, longest streak, today %, active/completed/remaining, motivation line, 7-day chart, today's checklist | done (upcoming goals arrive with Goals) |
| Habits: create / edit / delete (confirm) / pause / resume, daily / specific weekdays / every-N-days schedules, daily target, category, start date, photo-proof flag | done |
| Per-habit stats: current & longest streak, completed days, missed days, completion % | done |
| Photo proof: Android Photo Picker + camera, stored privately, linked to habit+date, optional note; proof-required habits can't be completed without a photo | done (viewing gallery arrives with Calendar) |
| Overall streak with configurable target (default 80%) | done |
| Settings: tracker name, your name, theme, overall target, week-start preference, app info | done |

Streak rules (see `domain/StreakEngine.kt`): everything is recomputed from raw records, so editing history
fixes streaks automatically; unscheduled/paused days are neutral and never break a streak; today stays
"pending" until it ends. Dates are stored as epoch days (no time-zone drift).

## Roadmap (not yet built – nothing is faked in the UI)

- Goals (short/medium/long, milestones, linked habits) and the Goals tab
- Calendar tab: monthly view, GitHub-style heatmap, day inspector with evidence, editing past days
- Analytics dashboard
- Reminders & notifications (WorkManager), notification preferences
- Backup/restore, CSV & PDF export, privacy settings
- Achievements
- Instrumented/UI tests

## Structure

```
app/src/main/java/com/kama/tracker/
  data/     Room entities, DAOs, database, repository, evidence file storage
  domain/   ScheduleRules, StreakEngine, ProgressCalculator (pure Kotlin, unit-tested)
  ui/       KamaRoot (nav + bottom bar), KamaViewModel, screens, components, theme
```
