package com.kama.tracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kama.tracker.domain.HabitProgress
import com.kama.tracker.ui.KamaViewModel
import com.kama.tracker.ui.components.CheckCircle
import com.kama.tracker.ui.components.EmptyState
import com.kama.tracker.ui.components.KamaCard
import com.kama.tracker.ui.components.ProgressRing
import com.kama.tracker.ui.components.StatTile
import com.kama.tracker.ui.components.WeeklyChart
import com.kama.tracker.ui.theme.KamaGreen
import com.kama.tracker.ui.theme.KamaOrange
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val MOTIVATION = listOf(
    "Discipline is choosing what you want most over what you want now.",
    "Small steps, taken every day, build the life you want.",
    "Don't break the chain.",
    "You don't need motivation. You need a system, and you're building one.",
    "Show up today. Future you is watching.",
    "Consistency beats intensity.",
    "One more rep, one more page, one more day.",
    "Progress is quiet. Keep going.",
    "Done is better than perfect.",
    "Win the morning, win the day.",
    "Every check mark is a vote for who you're becoming.",
    "Be stronger than your excuses.",
    "Start where you are. Use what you have.",
    "The best time to start was yesterday. The next best is now.",
)

@Composable
fun HomeScreen(vm: KamaViewModel, onOpenHabit: (Long) -> Unit, onAddHabit: () -> Unit) {
    val snapshot by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snap = snapshot
    val s = settings
    if (snap == null || s == null) return

    val today = LocalDate.ofEpochDay(snap.today)
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val name = if (s.userName.isBlank()) greeting else "$greeting, ${s.userName}"

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(s.trackerName.uppercase(), color = KamaOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
                Text(name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (snap.habits.isEmpty()) {
            item {
                KamaCard(Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No habits yet",
                        body = "Create your first habit to start building a streak.",
                    ) { Button(onClick = onAddHabit) { Text("Create a habit") } }
                }
            }
            return@LazyColumn
        }

        // Streaks + today's ring
        item {
            KamaCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(snap.todayPercent)
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text("Today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("${snap.todayDone} of ${snap.todayScheduled} done", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Spacer(Modifier.padding(top = 8.dp))
                        Text("🔥 ${snap.overall.current}-day streak", color = KamaOrange, fontWeight = FontWeight.Bold)
                        Text("Longest: ${snap.overall.longest} days", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Active habits", "${snap.activeHabits}", Modifier.weight(1f))
                StatTile("Completed", "${snap.todayDone}", Modifier.weight(1f), valueColor = KamaGreen)
                StatTile("Remaining", "${snap.remainingToday}", Modifier.weight(1f), valueColor = KamaOrange)
            }
        }

        item {
            KamaCard(Modifier.fillMaxWidth()) {
                Text(
                    "“" + MOTIVATION[(snap.today % MOTIVATION.size).toInt()] + "”",
                    modifier = Modifier.padding(18.dp), fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            KamaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("This week", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    WeeklyChart(snap.week, snap.today, s.overallTargetPercent)
                    Text(
                        "Green = reached your ${s.overallTargetPercent}% target",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        val todays = snap.habits.filter { it.scheduledToday }
        item { Text("Today's habits", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
        if (todays.isEmpty()) {
            item {
                Text(
                    "Nothing is scheduled for today. Enjoy the rest day - it won't break any streak.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(todays, key = { it.habit.id }) { h ->
                TodayRow(h, onTap = { vm.onHabitTapped(h) }, onOpen = { onOpenHabit(h.habit.id) })
            }
        }
    }
}

@Composable
private fun TodayRow(h: HabitProgress, onTap: () -> Unit, onOpen: () -> Unit) {
    KamaCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(checked = h.completedToday, partial = h.todayCount.toFloat(), onClick = onTap)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(h.habit.name, fontWeight = FontWeight.SemiBold)
                val extra = buildList {
                    if (h.habit.dailyTarget > 1) add("${h.todayCount}/${h.habit.dailyTarget}")
                    if (h.habit.proofRequired) add("photo proof")
                    if (h.summary.current > 0) add("🔥 ${h.summary.current}")
                }.joinToString(" · ")
                if (extra.isNotEmpty()) Text(extra, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
