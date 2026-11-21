package com.kama.tracker

import com.kama.tracker.data.CompletionEntity
import com.kama.tracker.data.HabitEntity
import com.kama.tracker.data.HabitPauseEntity
import com.kama.tracker.data.ScheduleType
import com.kama.tracker.domain.DayState
import com.kama.tracker.domain.ProgressCalculator
import com.kama.tracker.domain.StreakEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakEngineTest {
    // 2025-01-06 is a Monday.
    private val monday = LocalDate.of(2025, 1, 6).toEpochDay()

    private fun habit(
        id: Long = 1,
        start: Long = monday,
        type: String = ScheduleType.DAILY,
        mask: Int = 0b1111111,
        interval: Int = 1,
    ) = HabitEntity(id = id, name = "H$id", startDay = start, scheduleType = type, weekdayMask = mask, intervalDays = interval)

    private fun states(h: HabitEntity, done: Set<Long>, today: Long, pauses: List<HabitPauseEntity> = emptyList()) =
        StreakEngine.habitStates(h, pauses, done, today)

    @Test fun dailyStreakCountsConsecutiveDays() {
        val s = StreakEngine.summarize(states(habit(), setOf(monday, monday + 1, monday + 2), monday + 2))
        assertEquals(3, s.current); assertEquals(3, s.longest); assertEquals(0, s.missedDays)
    }

    @Test fun todayPendingDoesNotBreakStreak() {
        val s = StreakEngine.summarize(states(habit(), setOf(monday, monday + 1), monday + 2))
        assertEquals(2, s.current); assertEquals(0, s.missedDays)
    }

    @Test fun pastMissedDayBreaksStreakButKeepsLongest() {
        val s = StreakEngine.summarize(states(habit(), setOf(monday, monday + 1, monday + 3), monday + 3))
        assertEquals(1, s.current); assertEquals(2, s.longest); assertEquals(1, s.missedDays)
    }

    @Test fun unscheduledDaysDoNotBreakStreakOrCountAsMissed() {
        // Mon/Wed/Fri only (bits 0, 2, 4).
        val h = habit(type = ScheduleType.WEEKDAYS, mask = 0b0010101)
        val s = StreakEngine.summarize(states(h, setOf(monday, monday + 2, monday + 4), monday + 6))
        assertEquals(3, s.current); assertEquals(0, s.missedDays)
    }

    @Test fun everyNDaysSchedule() {
        val h = habit(type = ScheduleType.EVERY_N, interval = 3)
        val s = StreakEngine.summarize(states(h, setOf(monday, monday + 3), monday + 4))
        assertEquals(2, s.current); assertEquals(0, s.missedDays)
    }

    @Test fun pausedDaysAreNeutral() {
        val pause = HabitPauseEntity(habitId = 1, fromDay = monday + 1, toDay = monday + 4)
        val s = StreakEngine.summarize(states(habit(), setOf(monday, monday + 4), monday + 4, listOf(pause)))
        assertEquals(2, s.current); assertEquals(0, s.missedDays)
    }

    @Test fun editingHistoryRecalculates() {
        val h = habit()
        val before = StreakEngine.summarize(states(h, setOf(monday, monday + 2), monday + 2))
        val after = StreakEngine.summarize(states(h, setOf(monday, monday + 1, monday + 2), monday + 2))
        assertEquals(1, before.current)
        assertEquals(3, after.current)
    }

    @Test fun overallStreakUsesTargetPercent() {
        // 10 scheduled, 8 done at 80% => success; 7 done => missed.
        assertEquals(DayState.SUCCESS, StreakEngine.overallState(10, 8, 80))
        assertEquals(DayState.MISSED, StreakEngine.overallState(10, 7, 80))
        assertEquals(DayState.NEUTRAL, StreakEngine.overallState(0, 0, 80))
    }

    @Test fun overallProgressEndToEnd() {
        val a = habit(id = 1); val b = habit(id = 2)
        fun c(h: Long, d: Long) = CompletionEntity(habitId = h, day = d, count = 1, completed = true)
        // Day 0: both done, Day 1: only a done (50% < 80%), Day 2 (today): both done.
        val snap = ProgressCalculator.calculate(
            listOf(a, b), emptyList(),
            listOf(c(1, monday), c(2, monday), c(1, monday + 1), c(1, monday + 2), c(2, monday + 2)),
            80, monday + 2,
        )
        assertEquals(1, snap.overall.current)
        assertEquals(1, snap.overall.longest)
        assertEquals(2, snap.overall.successDays)
        assertEquals(1, snap.overall.missedDays)
        assertEquals(100, snap.todayPercent)
    }

    @Test fun emptyHabitListGivesZeroes() {
        val snap = ProgressCalculator.calculate(emptyList(), emptyList(), emptyList(), 80, monday)
        assertEquals(0, snap.overall.current)
        assertEquals(0, snap.todayScheduled)
        assertFalse(snap.week.any { it.percent != null })
        assertTrue(snap.week.size == 7)
    }
}
