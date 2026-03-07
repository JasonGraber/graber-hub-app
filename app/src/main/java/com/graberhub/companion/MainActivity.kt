package com.graberhub.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.graberhub.companion.ui.screens.*
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GraberHubTheme {
                GraberHubApp()
            }
        }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    Schedule("Schedule", Icons.Default.CalendarMonth),
    Chores("Chores", Icons.Default.CheckCircle),
    Timer("Timer", Icons.Default.Timer),
    Countdowns("Countdowns", Icons.Default.Flag),
    Settings("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraberHubApp(viewModel: MainViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(Tab.Schedule) }
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Graber Hub", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ShedBlueDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardBackground,
                tonalElevation = 8.dp
            ) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ShedBlue,
                            selectedTextColor = ShedBlue,
                            unselectedIconColor = TextHint,
                            unselectedTextColor = TextHint,
                            indicatorColor = ShedBlueLight
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                Tab.Schedule -> ScheduleScreen(viewModel)
                Tab.Chores -> ChoresScreen(viewModel)
                Tab.Timer -> TimerScreen(viewModel)
                Tab.Countdowns -> CountdownsScreen(viewModel)
                Tab.Settings -> SettingsScreen(viewModel)
            }
        }

        // Error snackbar
        error?.let { errorMsg ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                containerColor = ShedPink
            ) {
                Text(errorMsg, color = Color.White)
            }
        }
    }
}
