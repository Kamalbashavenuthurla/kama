package com.kama.tracker.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kama.tracker.domain.ScheduleRules
import com.kama.tracker.ui.KamaViewModel
import com.kama.tracker.ui.components.KamaCard
import com.kama.tracker.ui.components.StatTile
import com.kama.tracker.ui.theme.KamaGreen
import com.kama.tracker.ui.theme.KamaOrange
import com.kama.tracker.ui.theme.KamaRed
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(vm: KamaViewModel, habitId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val snapshot by vm.progress.collectAsStateWithLifecycle()
    val snap = snapshot
    val h = snap?.habits?.firstOrNull { it.habit.id == habitId }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }

    // Leave the screen once the habit is gone (deleted).
    LaunchedEffect(h, snap, deleted) { if (snap != null && h == null && deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(h?.habit?.name ?: "Habit") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        if (h == null) {
            if (snap != null && !deleted) Text("This habit no longer exists.", Modifier.padding(padding).padding(20.dp))
            return@Scaffold
        }
        val s = h.summary
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KamaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "${h.habit.category} · ${ScheduleRules.describe(h.habit)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (h.habit.description.isNotBlank()) Text(h.habit.description, Modifier.padding(top = 8.dp))
                    Text(
                        "Started ${LocalDate.ofEpochDay(h.habit.startDay).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}" +
                            " · target ${h.habit.dailyTarget}/day" +
                            if (h.habit.proofRequired) " · photo proof required" else "",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (h.isPaused) Text("Paused", color = KamaOrange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Current streak", "${s.current}", Modifier.weight(1f), KamaOrange)
                StatTile("Longest streak", "${s.longest}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Completed days", "${s.successDays}", Modifier.weight(1f), KamaGreen)
                StatTile("Missed days", "${s.missedDays}", Modifier.weight(1f), KamaRed)
                StatTile("Completion", "${s.percent}%", Modifier.weight(1f))
            }
            Button(onClick = { onEdit(h.habit.id) }, Modifier.fillMaxWidth()) { Text("Edit habit") }
            OutlinedButton(onClick = { vm.setPaused(h.habit.id, !h.isPaused) }, Modifier.fillMaxWidth()) {
                Text(if (h.isPaused) "Resume habit" else "Pause habit")
            }
            OutlinedButton(
                onClick = { confirmDelete = true },
                Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = KamaRed),
            ) { Text("Delete habit") }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this habit?") },
            text = { Text("All its history, streaks and photo evidence will be permanently removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    deleted = true
                    vm.deleteHabit(habitId)
                }) { Text("Delete", color = KamaRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
