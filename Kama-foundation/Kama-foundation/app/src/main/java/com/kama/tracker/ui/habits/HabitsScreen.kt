package com.kama.tracker.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kama.tracker.domain.ScheduleRules
import com.kama.tracker.ui.KamaViewModel
import com.kama.tracker.ui.components.EmptyState
import com.kama.tracker.ui.components.KamaCard
import com.kama.tracker.ui.theme.KamaOrange

@Composable
fun HabitsScreen(vm: KamaViewModel, onOpenHabit: (Long) -> Unit, onAddHabit: () -> Unit) {
    val snapshot by vm.progress.collectAsStateWithLifecycle()
    val snap = snapshot ?: return

    Box(Modifier.fillMaxSize()) {
        if (snap.habits.isEmpty()) {
            EmptyState(
                title = "No habits yet",
                body = "Habits you create appear here with their own streaks.",
                modifier = Modifier.align(Alignment.Center),
            ) { Button(onClick = onAddHabit) { Text("Create a habit") } }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("Habits", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                items(snap.habits, key = { it.habit.id }) { h ->
                    KamaCard(Modifier.fillMaxWidth(), onClick = { onOpenHabit(h.habit.id) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(h.habit.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(
                                    "${h.habit.category} · ${ScheduleRules.describe(h.habit)}" +
                                        if (h.isPaused) " · Paused" else "",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("🔥 ${h.summary.current}", color = KamaOrange, fontWeight = FontWeight.Bold)
                                Text("best ${h.summary.longest}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = onAddHabit,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = KamaOrange,
                contentColor = androidx.compose.ui.graphics.Color.Black,
            ) { Icon(Icons.Filled.Add, contentDescription = "Add habit") }
        }
    }
}
