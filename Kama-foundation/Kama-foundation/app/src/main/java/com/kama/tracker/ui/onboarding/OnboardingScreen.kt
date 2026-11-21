package com.kama.tracker.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kama.tracker.data.HabitEntity
import com.kama.tracker.ui.theme.KamaOrange
import java.time.LocalDate

/** Three steps: welcome -> personalise -> optional first habit. onFinish(userName, trackerName, firstHabit?). */
@Composable
fun OnboardingScreen(initialTrackerName: String, onFinish: (String, String, HabitEntity?) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var userName by rememberSaveable { mutableStateOf("") }
    var trackerName by rememberSaveable { mutableStateOf(initialTrackerName) }
    var habitName by rememberSaveable { mutableStateOf("") }
    var proof by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            0 -> {
                Text("KAMA", color = KamaOrange, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 4.sp)
                Spacer(Modifier.height(12.dp))
                Text("Build discipline.\nOne day at a time.", fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Track habits, keep streaks, attach photo proof and watch your progress grow. " +
                        "Everything stays on this phone and works offline.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp,
                )
                Spacer(Modifier.height(32.dp))
                Button({ step = 1 }, Modifier.fillMaxWidth()) { Text("Get started") }
            }
            1 -> {
                Text("Make it yours", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(userName, { userName = it.take(40) }, label = { Text("Your name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    trackerName, { trackerName = it.take(30) }, label = { Text("Name your tracker") },
                    supportingText = { Text("You can change this later in Settings.") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton({ step = 0 }) { Text("Back") }
                    Button({ step = 2 }, Modifier.weight(1f)) { Text("Next") }
                }
            }
            else -> {
                Text("Your first habit", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "It will repeat every day. You can change the schedule and target any time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(habitName, { habitName = it.take(60) }, label = { Text("e.g. Read 10 pages") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Require photo proof", Modifier.weight(1f))
                    Switch(proof, { proof = it })
                }
                Spacer(Modifier.height(24.dp))
                val finish = { first: HabitEntity? -> onFinish(userName, trackerName, first) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton({ step = 1 }) { Text("Back") }
                    Button(
                        onClick = {
                            finish(HabitEntity(name = habitName, startDay = LocalDate.now().toEpochDay(), proofRequired = proof))
                        },
                        enabled = habitName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Create & start") }
                }
                TextButton({ finish(null) }, Modifier.fillMaxWidth()) { Text("Skip for now") }
            }
        }
    }
}
