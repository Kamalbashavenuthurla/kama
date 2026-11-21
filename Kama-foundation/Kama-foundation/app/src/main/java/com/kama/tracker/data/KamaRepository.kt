package com.kama.tracker.data

import android.net.Uri
import com.kama.tracker.domain.ProgressCalculator
import com.kama.tracker.domain.ProgressSnapshot
import com.kama.tracker.domain.ScheduleRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class SetCountResult { OK, PROOF_REQUIRED, NOT_ALLOWED }

class KamaRepository(
    private val db: KamaDatabase,
    private val storage: EvidenceStorage,
) {
    private val habitDao = db.habitDao()
    private val pauseDao = db.pauseDao()
    private val completionDao = db.completionDao()
    private val evidenceDao = db.evidenceDao()
    private val settingsDao = db.settingsDao()

    val settings: Flow<SettingsEntity> = settingsDao.observe().filterNotNull()

    /** Emits today's epoch day now and again right after every local midnight. */
    private fun todayFlow(): Flow<Long> = flow {
        while (true) {
            emit(LocalDate.now().toEpochDay())
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(now, nextMidnight).toMillis() + 500)
        }
    }.distinctUntilChanged()

    /** Single source of truth for dashboard, habit list and habit detail. Recomputed whenever any input changes. */
    val progress: Flow<ProgressSnapshot> = combine(
        habitDao.observeAll(),
        pauseDao.observeAll(),
        completionDao.observeAll(),
        settings,
        todayFlow(),
    ) { habits, pauses, completions, s, today ->
        ProgressCalculator.calculate(habits, pauses, completions, s.overallTargetPercent, today)
    }.flowOn(Dispatchers.Default)

    suspend fun ensureDefaults() = settingsDao.insertIfMissing(SettingsEntity())

    // ---------------- settings ----------------

    suspend fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) {
        val current = settingsDao.get() ?: SettingsEntity()
        val next = transform(current).let {
            it.copy(
                trackerName = it.trackerName.trim().ifBlank { "Kama" }.take(30),
                userName = it.userName.trim().take(40),
                overallTargetPercent = it.overallTargetPercent.coerceIn(10, 100),
            )
        }
        settingsDao.save(next)
    }

    // ---------------- habits ----------------

    /** Inserts (id == 0) or updates a habit. Returns an error message, or null on success. */
    suspend fun saveHabit(habit: HabitEntity): String? {
        ScheduleRules.validate(habit)?.let { return it }
        val cleaned = habit.copy(
            name = habit.name.trim(),
            description = habit.description.trim(),
            category = habit.category.trim().ifBlank { "General" },
        )
        if (cleaned.id == 0L) habitDao.insert(cleaned) else habitDao.update(cleaned)
        return null
    }

    suspend fun deleteHabit(id: Long) {
        // Rows cascade; image files do not, so remove them explicitly.
        val paths = evidenceDao.pathsForHabit(id)
        habitDao.delete(id)
        storage.deleteAll(paths)
    }

    suspend fun setPaused(habitId: Long, paused: Boolean, today: Long) {
        val open = pauseDao.openFor(habitId)
        if (paused && open == null) {
            pauseDao.insert(HabitPauseEntity(habitId = habitId, fromDay = today))
        } else if (!paused && open != null) {
            // Pausing and resuming on the same day leaves an empty range; harmless.
            pauseDao.update(open.copy(toDay = today))
        }
    }

    // ---------------- completion ----------------

    /**
     * Sets how many times [habitId] was done on [day] (history can be edited for any past day).
     * Completion is true when count >= dailyTarget. If the habit demands photo proof, completing
     * without at least one photo for that day returns PROOF_REQUIRED and changes nothing.
     */
    suspend fun setCount(habitId: Long, day: Long, count: Int, today: Long): SetCountResult {
        val habit = habitDao.get(habitId) ?: return SetCountResult.NOT_ALLOWED
        if (day > today || day < habit.startDay) return SetCountResult.NOT_ALLOWED
        if (!ScheduleRules.isScheduled(habit, pauseDao.forHabit(habitId), day)) return SetCountResult.NOT_ALLOWED

        val clamped = count.coerceIn(0, habit.dailyTarget)
        val completed = clamped >= habit.dailyTarget
        if (completed && habit.proofRequired && evidenceDao.count(habitId, day) == 0) {
            return SetCountResult.PROOF_REQUIRED
        }
        completionDao.setCount(habitId, day, clamped, completed)
        return SetCountResult.OK
    }

    // ---------------- evidence ----------------

    /** Copies the image into private storage and links it to the habit and date. */
    suspend fun addEvidence(habitId: Long, day: Long, source: Uri, note: String) {
        val relative = storage.import(source, habitId, day)
        evidenceDao.insert(EvidenceEntity(habitId = habitId, day = day, filePath = relative, note = note.trim().take(500)))
    }

    fun clearCameraCache() = storage.clearCameraCache()
    fun newCameraTarget() = storage.newCameraTarget()
}
