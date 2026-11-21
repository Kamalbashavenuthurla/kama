package com.kama.tracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kama.tracker.BuildConfig
import com.kama.tracker.ui.KamaViewModel
import com.kama.tracker.ui.components.KamaCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: KamaViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return

    var tracker by remember(s.trackerName) { mutableStateOf(s.trackerName) }
    var user by remember(s.userName) { mutableStateOf(s.userName) }
    var target by remember(s.overallTargetPercent) { mutableFloatStateOf(s.overallTargetPercent.toFloat()) }

    // Persist pending text edits when the screen is left (e.g. user switches tab without unfocusing).
    val pending by rememberUpdatedState(Triple(user, tracker, s))
    DisposableEffect(Unit) {
        onDispose {
            val (u, t, current) = pending
            if (u != current.userName || t != current.trackerName) {
                vm.updateSettings { c -> c.copy(userName = u, trackerName = t) }
            }
        }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Profile & settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Section("Personal") {
            OutlinedTextField(
                user, { user = it.take(40) }, label = { Text("Your name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused && user != s.userName) vm.updateSettings { c -> c.copy(userName = user) } },
            )
            OutlinedTextField(
                tracker, { tracker = it.take(30) }, label = { Text("Tracker name") }, singleLine = true,
                supportingText = { Text("Shown on the Home screen. Empty resets to \"Kama\".") },
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused && tracker != s.trackerName) vm.updateSettings { c -> c.copy(trackerName = tracker) } },
            )
        }

        Section("Appearance") {
            Choice(listOf("DARK" to "Dark", "LIGHT" to "Light", "SYSTEM" to "System"), s.theme) { v -> vm.updateSettings { it.copy(theme = v) } }
        }

        Section("Overall streak target") {
            Text(
                "A day counts toward your overall streak when you complete at least ${target.toInt()}% of the habits scheduled for it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            )
            Slider(
                value = target, onValueChange = { target = it },
                onValueChangeFinished = { vm.updateSettings { it.copy(overallTargetPercent = target.toInt()) } },
                valueRange = 10f..100f, steps = 17,
            )
            Text("${target.toInt()}%", fontWeight = FontWeight.Bold)
        }

        Section("Week starts on") {
            Choice(listOf("1" to "Mon", "7" to "Sun", "6" to "Sat"), s.weekStart.toString()) { v ->
                vm.updateSettings { it.copy(weekStart = v.toInt()) }
            }
        }

        Section("About") {
            Text("KAMA – Discipline & Progress Tracker")
            Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("All data is stored on this device only.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    KamaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}
