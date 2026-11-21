package com.kama.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kama.tracker.data.HabitEntity
import com.kama.tracker.data.KamaRepository
import com.kama.tracker.data.SetCountResult
import com.kama.tracker.data.SettingsEntity
import com.kama.tracker.domain.HabitProgress
import com.kama.tracker.domain.ProgressSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri

/** Asks the UI to collect a photo before [habitId] can be completed for [day]. */
data class ProofRequest(val habitId: Long, val habitName: String, val day: Long, val targetCount: Int)

class KamaViewModel(private val repo: KamaRepository) : ViewModel() {

    /** null until the first database read completes (drives the loading state). */
    val settings: StateFlow<SettingsEntity?> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val progress: StateFlow<ProgressSnapshot?> =
        repo.progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _proofRequest = MutableStateFlow<ProofRequest?>(null)
    val proofRequest: StateFlow<ProofRequest?> = _proofRequest.asStateFlow()

    private fun message(text: String) { _events.tryEmit(text) }

    private fun guarded(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                message("Something went wrong: ${e.message ?: "unknown error"}")
            }
        }
    }

    // ---- settings / onboarding ----

    fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) = guarded { repo.updateSettings(transform) }

    fun completeOnboarding(userName: String, trackerName: String, firstHabit: HabitEntity?) = guarded {
        if (firstHabit != null) {
            repo.saveHabit(firstHabit)?.let { message(it); return@guarded }
        }
        repo.updateSettings { it.copy(userName = userName, trackerName = trackerName, onboardingDone = true) }
    }

    // ---- habits ----

    fun saveHabit(habit: HabitEntity, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = try {
                repo.saveHabit(habit)
            } catch (e: Exception) {
                "Could not save the habit: ${e.message ?: "unknown error"}"
            }
            onResult(error)
        }
    }

    fun deleteHabit(id: Long) = guarded {
        repo.deleteHabit(id)
        message("Habit deleted")
    }

    fun setPaused(habitId: Long, paused: Boolean) = guarded {
        val today = progress.value?.today ?: java.time.LocalDate.now().toEpochDay()
        repo.setPaused(habitId, paused, today)
        message(if (paused) "Habit paused" else "Habit resumed")
    }

    // ---- completion & proof ----

    /** Tap on a today-row: +1 towards the target; tapping a completed habit resets it to 0 (undo). */
    fun onHabitTapped(h: HabitProgress) = guarded {
        val today = progress.value?.today ?: return@guarded
        val next = if (h.completedToday) 0 else h.todayCount + 1
        when (repo.setCount(h.habit.id, today, next, today)) {
            SetCountResult.OK -> Unit
            SetCountResult.PROOF_REQUIRED ->
                _proofRequest.value = ProofRequest(h.habit.id, h.habit.name, today, next)
            SetCountResult.NOT_ALLOWED -> message("This habit isn't scheduled today.")
        }
    }

    fun dismissProof() { _proofRequest.value = null; repo.clearCameraCache() }

    fun newCameraTarget() = repo.newCameraTarget()

    /** Stores the photo + note, then completes the habit. */
    fun submitProof(request: ProofRequest, photo: Uri, note: String) = guarded {
        repo.addEvidence(request.habitId, request.day, photo, note)
        val today = progress.value?.today ?: request.day
        when (repo.setCount(request.habitId, request.day, request.targetCount, today)) {
            SetCountResult.OK -> message("${request.habitName} completed")
            else -> message("Photo saved, but the habit could not be completed.")
        }
        dismissProof()
    }
}
