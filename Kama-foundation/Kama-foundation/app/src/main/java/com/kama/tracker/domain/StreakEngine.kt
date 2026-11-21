package com.kama.tracker.domain

import com.kama.tracker.data.HabitEntity
import com.kama.tracker.data.HabitPauseEntity

enum class DayState { SUCCESS, MISSED, NEUTRAL }

data class StreakSummary(
    val current: Int = 0,
    val longest: Int = 0,
    val successDays: Int = 0,
    val missedDays: Int = 0,
) {
    /** Completion rate over scheduled, finished days; 0 when nothing has been scheduled yet. */
    val percent: Int
        get() = if (successDays + missedDays == 0) 0 else successDays * 100 / (successDays + missedDays)
}

/**
 * Pure, side-effect-free streak maths. Everything is recomputed from raw records,
 * so editing a historical completion automatically corrects every streak and statistic.
 */
object StreakEngine {

    /**
     * Day states for a habit from its start day up to and including [today].
     * NEUTRAL = not scheduled (paused, before start, off-schedule) and therefore never breaks a streak.
     */
    fun habitStates(
        habit: HabitEntity,
        pauses: List<HabitPauseEntity>,
        completedDays: Set<Long>,
        today: Long,
    ): List<DayState> {
        if (today < habit.startDay) return emptyList()
        return (habit.startDay..today).map { day ->
            when {
                !ScheduleRules.isScheduled(habit, pauses, day) -> DayState.NEUTRAL
                day in completedDays -> DayState.SUCCESS
                else -> DayState.MISSED
            }
        }
    }

    /**
     * [states] must be ordered by day with the LAST element being today.
     * If today is MISSED it is still pending (the day is not over), so it neither breaks
     * the streak nor counts as a missed day.
     */
    fun summarize(states: List<DayState>): StreakSummary {
        val finished = if (states.lastOrNull() == DayState.MISSED) states.dropLast(1) else states
        var run = 0
        var longest = 0
        var success = 0
        var missed = 0
        for (s in finished) {
            when (s) {
                DayState.SUCCESS -> {
                    run++
                    success++
                    if (run > longest) longest = run
                }
                DayState.MISSED -> {
                    run = 0
                    missed++
                }
                DayState.NEUTRAL -> Unit
            }
        }
        return StreakSummary(current = run, longest = longest, successDays = success, missedDays = missed)
    }

    /** Overall daily state: a day succeeds when completed/scheduled >= target percent. No scheduled habits = NEUTRAL. */
    fun overallState(scheduled: Int, done: Int, targetPercent: Int): DayState = when {
        scheduled == 0 -> DayState.NEUTRAL
        done * 100 >= targetPercent * scheduled -> DayState.SUCCESS
        else -> DayState.MISSED
    }
}
