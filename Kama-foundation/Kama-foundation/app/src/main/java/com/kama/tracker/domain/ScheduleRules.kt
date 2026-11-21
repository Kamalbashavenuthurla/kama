package com.kama.tracker.domain

import com.kama.tracker.data.HabitEntity
import com.kama.tracker.data.HabitPauseEntity
import com.kama.tracker.data.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object ScheduleRules {

    /** Whether [habit] is expected to be done on [day] (epoch day). Unscheduled days never count as missed. */
    fun isScheduled(habit: HabitEntity, pauses: List<HabitPauseEntity>, day: Long): Boolean {
        if (day < habit.startDay) return false
        if (pauses.any { day >= it.fromDay && (it.toDay == null || day < it.toDay) }) return false
        return when (habit.scheduleType) {
            ScheduleType.WEEKDAYS -> {
                val bit = LocalDate.ofEpochDay(day).dayOfWeek.value - 1
                (habit.weekdayMask shr bit) and 1 == 1
            }
            ScheduleType.EVERY_N -> habit.intervalDays > 0 && (day - habit.startDay) % habit.intervalDays == 0L
            else -> true
        }
    }

    fun describe(habit: HabitEntity): String = when (habit.scheduleType) {
        ScheduleType.WEEKDAYS -> {
            val full = habit.weekdayMask == 0b1111111
            if (full) "Every day" else (0..6)
                .filter { (habit.weekdayMask shr it) and 1 == 1 }
                .joinToString(", ") { DayOfWeek.of(it + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        }
        ScheduleType.EVERY_N -> if (habit.intervalDays == 1) "Every day" else "Every ${habit.intervalDays} days"
        else -> "Every day"
    }

    /** Returns an error message, or null when the habit is valid. */
    fun validate(h: HabitEntity): String? = when {
        h.name.isBlank() -> "Please enter a habit name."
        h.name.trim().length > 60 -> "Habit name must be 60 characters or fewer."
        h.description.length > 500 -> "Description must be 500 characters or fewer."
        h.dailyTarget !in 1..1000 -> "Daily target must be between 1 and 1000."
        h.scheduleType == ScheduleType.WEEKDAYS && (h.weekdayMask and 0b1111111) == 0 -> "Select at least one weekday."
        h.scheduleType == ScheduleType.EVERY_N && h.intervalDays !in 1..365 -> "Interval must be between 1 and 365 days."
        else -> null
    }
}
