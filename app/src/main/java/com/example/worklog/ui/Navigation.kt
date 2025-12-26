package com.example.worklog.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.worklog.WorkLogApplication
import com.example.worklog.ui.calculator.CalculatorScreen
import com.example.worklog.ui.home.HomeScreen
import com.example.worklog.ui.home.HomeViewModel
import com.example.worklog.ui.settings.AllSessionsScreen
import com.example.worklog.ui.settings.SettingsScreen
import com.example.worklog.ui.settings.SettingsViewModel
import com.example.worklog.ui.stats.StatsScreen
import com.example.worklog.ui.stats.StatsViewModel
import java.time.LocalTime

@Composable
fun viewModelFactory(): ViewModelFactory {
    val application = LocalContext.current.applicationContext as WorkLogApplication
    return ViewModelFactory(application.container)
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Stats : Screen("stats", "统计", Icons.AutoMirrored.Filled.ShowChart)
    object Calculator : Screen("calculator", "计算器", Icons.Default.Calculate)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
    object AllSessions : Screen("all_sessions", "All Sessions", Icons.AutoMirrored.Filled.ArrowBack)
}

val items = listOf(
    Screen.Home,
    Screen.Stats,
    Screen.Calculator,
    Screen.Settings
)

@Composable
fun MainScreen(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController, 
            startDestination = Screen.Home.route, 
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory())
                val uiState by homeViewModel.uiState.collectAsState()
                HomeScreen(
                    isWorking = uiState.isWorking,
                    timer = uiState.timer,
                    onToggleWork = { homeViewModel.toggleWork() },
                    annualSalary = uiState.annualSalary,
                    isSalaryVisible = uiState.isSalaryVisible,
                    onToggleSalaryVisibility = { homeViewModel.toggleSalaryVisibility() }
                )
            }
            composable(Screen.Stats.route) {
                val statsViewModel: StatsViewModel = viewModel(factory = viewModelFactory())
                StatsScreen(viewModel = statsViewModel)
            }
            composable(Screen.Calculator.route) { CalculatorScreen() }
            composable(Screen.Settings.route) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                
                val context = LocalContext.current
                
                SettingsScreen(
                    uiState = settingsUiState,
                    onUpdateAnnualSalary = { value: TextFieldValue -> settingsViewModel.updateAnnualSalary(value.text) },
                    onUpdateTheme = { settingsViewModel.updateTheme(it) },
                    onUpdateStartTime = { time: LocalTime -> settingsViewModel.updateStartTime(time) },
                    onUpdateEndTime = { time: LocalTime -> settingsViewModel.updateEndTime(time) },
                    onDeleteSession = { settingsViewModel.deleteSession(it) },
                    onUpdateSessionNote = { session, note -> settingsViewModel.updateSessionNote(session, note) },
                    onBackupData = { uri -> settingsViewModel.backupData(context, uri) },
                    onImportData = { uri -> settingsViewModel.importData(context, uri) },
                    onCleanInvalidData = { settingsViewModel.cleanInvalidData(context) },
                    onNavigateToAllSessions = { navController.navigate(Screen.AllSessions.route) }
                )
            }
            composable(Screen.AllSessions.route) {
                AllSessionsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}