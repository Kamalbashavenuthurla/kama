package com.kama.tracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import com.kama.tracker.ui.habits.HabitDetailScreen
import com.kama.tracker.ui.habits.HabitEditorScreen
import com.kama.tracker.ui.habits.HabitsScreen
import com.kama.tracker.ui.home.HomeScreen
import com.kama.tracker.ui.onboarding.OnboardingScreen
import com.kama.tracker.ui.proof.ProofSheet
import com.kama.tracker.ui.settings.SettingsScreen
import com.kama.tracker.ui.theme.KamaTheme

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Habits("habits", "Habits", Icons.Filled.TaskAlt),
    Settings("settings", "Profile", Icons.Filled.Settings),
}

@Composable
fun KamaRoot(vm: KamaViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings

    KamaTheme(mode = s?.theme ?: "DARK") {
        Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
            when {
                s == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                !s.onboardingDone -> OnboardingScreen(
                    initialTrackerName = s.trackerName,
                    onFinish = { user, tracker, habit -> vm.completeOnboarding(user, tracker, habit) },
                )
                else -> MainScaffold(vm)
            }
        }
    }
}

@Composable
private fun MainScaffold(vm: KamaViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val proof by vm.proofRequest.collectAsState()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LaunchedEffect(Unit) { vm.events.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (Tab.values().any { it.route == route }) {
                NavigationBar {
                    Tab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = Tab.Home.route,
            // consumeWindowInsets stops the nested Scaffolds (detail/editor) applying the insets twice.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Tab.Home.route) {
                HomeScreen(
                    vm = vm,
                    onOpenHabit = { nav.navigate("habit/$it") },
                    onAddHabit = { nav.navigate("edit/-1") },
                )
            }
            composable(Tab.Habits.route) {
                HabitsScreen(vm, onOpenHabit = { nav.navigate("habit/$it") }, onAddHabit = { nav.navigate("edit/-1") })
            }
            composable(Tab.Settings.route) { SettingsScreen(vm) }
            composable("habit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                HabitDetailScreen(
                    vm = vm,
                    habitId = it.arguments?.getLong("id") ?: -1L,
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate("edit/$id") },
                )
            }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                HabitEditorScreen(vm, habitId = it.arguments?.getLong("id") ?: -1L, onDone = { nav.popBackStack() })
            }
        }
    }

    proof?.let { request -> ProofSheet(request = request, vm = vm) }
}
