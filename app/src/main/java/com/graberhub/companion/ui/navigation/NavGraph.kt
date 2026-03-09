package com.graberhub.companion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.graberhub.companion.ui.screens.*
import com.graberhub.companion.viewmodel.MainViewModel

sealed class Screen(val route: String, val label: String, val emoji: String) {
    data object Schedule : Screen("schedule", "Schedule", "📅")
    data object Chores : Screen("chores", "Chores", "✅")
    data object Countdowns : Screen("countdowns", "Countdowns", "⏳")
    data object Timer : Screen("timer", "Timer", "⏱️")
    data object Settings : Screen("settings", "Settings", "⚙️")
    data object ManageChores : Screen("manage_chores", "Manage Chores", "📋")
}

@Composable
fun NavGraph(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(navController = navController, startDestination = Screen.Schedule.route) {
        composable(Screen.Schedule.route) { ScheduleScreen(viewModel) }
        composable(Screen.Chores.route) { ChoresScreen(viewModel, navController) }
        composable(Screen.Countdowns.route) { CountdownsScreen(viewModel) }
        composable(Screen.Timer.route) { TimerScreen(viewModel) }
        composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        composable(Screen.ManageChores.route) { ManageChoresScreen(viewModel, navController) }
    }
}
