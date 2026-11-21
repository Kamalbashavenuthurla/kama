package com.kama.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        HabitEntity::class,
        HabitPauseEntity::class,
        CompletionEntity::class,
        EvidenceEntity::class,
        GoalEntity::class,
        MilestoneEntity::class,
        GoalHabitCrossRef::class,
        ReminderEntity::class,
        AchievementEntity::class,
        SettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KamaDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun pauseDao(): PauseDao
    abstract fun completionDao(): CompletionDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        fun build(context: Context): KamaDatabase =
            Room.databaseBuilder(context.applicationContext, KamaDatabase::class.java, "kama.db")
                // Never fall back to destructive migration: user progress must not be wiped.
                .addMigrations(*Migrations.ALL)
                .build()
    }
}

/**
 * Add one Migration per schema bump, e.g.
 *   val M_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { ... } }
 * and register it in ALL. Schemas are exported to app/schemas for MigrationTestHelper.
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
