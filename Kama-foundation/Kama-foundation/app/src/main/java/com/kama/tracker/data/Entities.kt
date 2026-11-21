package com.kama.tracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Date handling: every calendar date is stored as a Long "epoch day"
 * (java.time.LocalDate.toEpochDay()). It has no time zone and no time-of-day,
 * so a completion can never drift to the wrong date when the zone or clock changes.
 */

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "General",
    val startDay: Long,
    /** DAILY, WEEKDAYS (specific weekdays, see weekdayMask) or EVERY_N (every intervalDays days). */
    val scheduleType: String = ScheduleType.DAILY,
    /** Bit 0 = Monday ... bit 6 = Sunday. Used when scheduleType == WEEKDAYS. */
    val weekdayMask: Int = 0b1111111,
    val intervalDays: Int = 1,
    val dailyTarget: Int = 1,
    val proofRequired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

object ScheduleType {
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS"
    const val EVERY_N = "EVERY_N"
}

/** A period in which a habit was paused. toDay is the first day active again (exclusive); null = still paused. */
@Entity(
    tableName = "habit_pauses",
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class HabitPauseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val fromDay: Long,
    val toDay: Long? = null,
)

/** One row per habit per day (unique index) - this is what prevents duplicate daily records. */
@Entity(
    tableName = "completions",
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["habitId", "day"], unique = true)],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val day: Long,
    val count: Int,
    val completed: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** filePath is relative to filesDir/evidence so the record survives backup/restore. */
@Entity(
    tableName = "evidence",
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["habitId", "day"])],
)
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val day: Long,
    val filePath: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

// ---- Tables created now so database version 1 is complete; their DAOs/UI arrive in later steps. ----

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** SHORT (1-30 days), MEDIUM (1-6 months), LONG (6 months - 5 years). */
    val term: String,
    val startDay: Long,
    val deadlineDay: Long,
    val manualProgress: Int = 0,
    val completedAt: Long? = null,
)

@Entity(
    tableName = "milestones",
    foreignKeys = [ForeignKey(GoalEntity::class, ["id"], ["goalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("goalId")],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val title: String,
    val done: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "goal_habits",
    primaryKeys = ["goalId", "habitId"],
    foreignKeys = [
        ForeignKey(GoalEntity::class, ["id"], ["goalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("habitId")],
)
data class GoalHabitCrossRef(val goalId: Long, val habitId: Long)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** HABIT, DAILY_SUMMARY, INCOMPLETE, GOAL */
    val type: String,
    val refId: Long? = null,
    val minuteOfDay: Int,
    val weekdayMask: Int = 0b1111111,
    val enabled: Boolean = true,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,
    val unlockedAt: Long,
)

/** Single-row table (id is always 1). */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val trackerName: String = "Kama",
    val userName: String = "",
    /** DARK, LIGHT or SYSTEM */
    val theme: String = "DARK",
    val overallTargetPercent: Int = 80,
    /** java.time.DayOfWeek.getValue(): 1 = Monday ... 7 = Sunday */
    val weekStart: Int = 1,
    val onboardingDone: Boolean = false,
)
