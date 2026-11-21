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
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kama.tracker.data.HabitEntity
import com.kama.tracker.data.ScheduleType
import com.kama.tracker.ui.KamaViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CATEGORIES = listOf("General", "Health", "Fitness", "Learning", "Work", "Mindfulness", "Finance", "Social")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(vm: KamaViewModel, habitId: Long, onDone: () -> Unit) {
    val snapshot by vm.progress.collectAsStateWithLifecycle()
    val snap = snapshot ?: return
    val existing = snap.habits.firstOrNull { it.habit.id == habitId }?.habit
    if (habitId != -1L && existing == null) {
        Text("This habit no longer exists.", Modifier.padding(20.dp))
        return
    }
    val isNew = existing == null

    var name by rememberSaveable(habitId) { mutableStateOf(existing?.name ?: "") }
    var description by rememberSaveable(habitId) { mutableStateOf(existing?.description ?: "") }
    var category by rememberSaveable(habitId) { mutableStateOf(existing?.category ?: "General") }
    var startDay by rememberSaveable(habitId) { mutableStateOf(existing?.startDay ?: snap.today) }
    var scheduleType by rememberSaveable(habitId) { mutableStateOf(existing?.scheduleType ?: ScheduleType.DAILY) }
    var mask by rememberSaveable(habitId) { mutableStateOf(existing?.weekdayMask ?: 0b1111111) }
    var interval by rememberSaveable(habitId) { mutableStateOf((existing?.intervalDays ?: 2).toString()) }
    var target by rememberSaveable(habitId) { mutableStateOf((existing?.dailyTarget ?: 1).toString()) }
    var proof by rememberSaveable(habitId) { mutableStateOf(existing?.proofRequired ?: false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New habit" else "Edit habit") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(name, { name = it.take(60) }, label = { Text("Habit name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it.take(500) }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())

            Text("Category", style = MaterialTheme.typography.labelLarge)
            ChipRows(CATEGORIES, selected = category) { category = it }

            OutlinedButton(onClick = { showDate = true }, Modifier.fillMaxWidth()) {
                Text("Start date: " + LocalDate.ofEpochDay(startDay).format(DateTimeFormatter.ofPattern("d MMM yyyy")))
            }

            Text("Schedule", style = MaterialTheme.typography.labelLarge)
            ChipRows(listOf("Daily", "Specific days", "Every N days"), selected = when (scheduleType) {
                ScheduleType.WEEKDAYS -> "Specific days"
                ScheduleType.EVERY_N -> "Every N days"
                else -> "Daily"
            }) {
                scheduleType = when (it) {
                    "Specific days" -> ScheduleType.WEEKDAYS
                    "Every N days" -> ScheduleType.EVERY_N
                    else -> ScheduleType.DAILY
                }
            }
            if (scheduleType == ScheduleType.WEEKDAYS) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..6).forEach { i ->
                        FilterChip(
                            selected = (mask shr i) and 1 == 1,
                            onClick = { mask = mask xor (1 shl i) },
                            label = { Text(DayOfWeek.of(i + 1).getDisplayName(TextStyle.NARROW, Locale.getDefault())) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (scheduleType == ScheduleType.EVERY_N) {
                OutlinedTextField(
                    interval, { interval = it.filter(Char::isDigit).take(3) },
                    label = { Text("Repeat every (days)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                target, { target = it.filter(Char::isDigit).take(4) },
                label = { Text("Daily target (times per scheduled day)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Require photo proof")
                    Text("Completing this habit needs a photo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(proof, { proof = it })
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val habit = (existing ?: HabitEntity(name = "", startDay = startDay)).copy(
                        name = name,
                        description = description,
                        category = category,
                        startDay = startDay,
                        scheduleType = scheduleType,
                        weekdayMask = mask,
                        intervalDays = interval.toIntOrNull() ?: 0,
                        dailyTarget = target.toIntOrNull() ?: 0,
                        proofRequired = proof,
                    )
                    saving = true
                    vm.saveHabit(habit) { err ->
                        saving = false
                        if (err == null) onDone() else error = err
                    }
                },
            ) { Text(if (isNew) "Create habit" else "Save changes") }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        startDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

/** Simple two-per-row-wrapping chip layout without experimental APIs: 4 chips per row. */
@Composable
private fun ChipRows(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(if (options.size <= 3) 3 else 4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(o) }) }
            }
        }
    }
}
