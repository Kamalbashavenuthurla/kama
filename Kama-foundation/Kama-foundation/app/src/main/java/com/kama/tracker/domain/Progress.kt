package com.kama.tracker.domain

import com.kama.tracker.data.CompletionEntity
import com.kama.tracker.data.HabitEntity
import com.kama.tracker.data.HabitPauseEntity

data class HabitProgress(
    val habit: HabitEntity,
    val isPaused: Boolean,
    val scheduledToday: Boolean,
    val todayCount: Int,
    val completedToday: Boolean,
    val summary: StreakSummary,
)

/** percent == null means nothing was scheduled that day. */
data class DayBar(val day: Long, val percent: Int?)

data class ProgressSnapshot(
    val today: Long,
    val habits: List<HabitProgress>,
    val overall: StreakSummary,
    val todayScheduled: Int,
    val todayDone: Int,
    val week: List<DayBar>,
) {
    val todayPercent: Int get() = if (todayScheduled == 0) 0 else todayDone * 100 / todayScheduled
    val activeHabits: Int get() = habits.count { !it.isPaused }
    val remainingToday: Int get() = todayScheduled - todayDone
}

object ProgressCalculator {

    fun calculate(
        habits: List<HabitEntity>,
        pauses: List<HabitPauseEntity>,
        completions: List<CompletionEntity>,
        targetPercent: Int,
        today: Long,
    ): ProgressSnapshot {
        val pausesByHabit = pauses.groupBy { it.habitId }
        val doneByHabit: Map<Long, Set<Long>> = completions.filter { it.completed }
            .groupBy({ it.habitId }, { it.day })
            .mapValues { it.value.toSet() }
        val countByKey = completions.associate { (it.habitId to it.day) to it.count }

        val habitProgress = habits.map { h ->
            val ps = pausesByHabit[h.id].orEmpty()
            val done = doneByHabit[h.id].orEmpty()
            HabitProgress(
                habit = h,
                isPaused = ps.any { it.toDay == null },
                scheduledToday = ScheduleRules.isScheduled(h, ps, today),
                todayCount = countByKey[h.id to today] ?: 0,
                completedToday = today in done,
                summary = StreakEngine.summarize(StreakEngine.habitStates(h, ps, done, today)),
            )
        }

        fun dayCounts(day: Long): Pair<Int, Int> {
            var scheduled = 0
            var done = 0
            for (h in habits) {
                if (ScheduleRules.isScheduled(h, pausesByHabit[h.id].orEmpty(), day)) {
                    scheduled++
                    if (day in doneByHabit[h.id].orEmpty()) done++
                }
            }
            return scheduled to done
        }

        val overallStates = if (habits.isEmpty()) emptyList() else {
            val first = habits.minOf { it.startDay }
            if (first > today) emptyList() else (first..today).map { d ->
                val (s, c) = dayCounts(d)
                StreakEngine.overallState(s, c, targetPercent)
            }
        }

        val (todayScheduled, todayDone) = dayCounts(today)
        val week = (today - 6..today).map { d ->
            val (s, c) = dayCounts(d)
            DayBar(d, if (s == 0) null else c * 100 / s)
        }

        return ProgressSnapshot(
            today = today,
            habits = habitProgress,
            overall = StreakEngine.summarize(overallStates),
            todayScheduled = todayScheduled,
            todayDone = todayDone,
            week = week,
        )
    }
}
