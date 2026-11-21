package com.kama.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt, id")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: Long): HabitEntity?

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PauseDao {
    @Query("SELECT * FROM habit_pauses")
    fun observeAll(): Flow<List<HabitPauseEntity>>

    @Query("SELECT * FROM habit_pauses WHERE habitId = :habitId")
    suspend fun forHabit(habitId: Long): List<HabitPauseEntity>

    @Query("SELECT * FROM habit_pauses WHERE habitId = :habitId AND toDay IS NULL LIMIT 1")
    suspend fun openFor(habitId: Long): HabitPauseEntity?

    @Insert
    suspend fun insert(pause: HabitPauseEntity): Long

    @Update
    suspend fun update(pause: HabitPauseEntity)
}

@Dao
abstract class CompletionDao {
    @Query("SELECT * FROM completions")
    abstract fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE habitId = :habitId AND day = :day")
    abstract suspend fun get(habitId: Long, day: Long): CompletionEntity?

    @Insert
    abstract suspend fun insert(c: CompletionEntity): Long

    @Update
    abstract suspend fun update(c: CompletionEntity)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND day = :day")
    abstract suspend fun delete(habitId: Long, day: Long)

    /** Insert-or-update the single row for (habit, day); count 0 removes the row entirely. */
    @Transaction
    open suspend fun setCount(habitId: Long, day: Long, count: Int, completed: Boolean) {
        if (count <= 0) {
            delete(habitId, day)
            return
        }
        val existing = get(habitId, day)
        val now = System.currentTimeMillis()
        if (existing == null) {
            insert(CompletionEntity(habitId = habitId, day = day, count = count, completed = completed, updatedAt = now))
        } else {
            update(existing.copy(count = count, completed = completed, updatedAt = now))
        }
    }
}

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence ORDER BY day DESC, createdAt DESC")
    fun observeAll(): Flow<List<EvidenceEntity>>

    @Query("SELECT COUNT(*) FROM evidence WHERE habitId = :habitId AND day = :day")
    suspend fun count(habitId: Long, day: Long): Int

    @Query("SELECT * FROM evidence WHERE id = :id")
    suspend fun get(id: Long): EvidenceEntity?

    @Query("SELECT filePath FROM evidence WHERE habitId = :habitId")
    suspend fun pathsForHabit(habitId: Long): List<String>

    @Insert
    suspend fun insert(e: EvidenceEntity): Long

    @Query("DELETE FROM evidence WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(s: SettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(s: SettingsEntity)
}
