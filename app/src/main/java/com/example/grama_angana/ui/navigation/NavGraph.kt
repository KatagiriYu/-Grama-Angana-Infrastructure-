package com.example.grama_angana.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.grama_angana.R
import com.example.grama_angana.ui.MainViewModel
import com.example.grama_angana.ui.screens.*
import kotlinx.coroutines.launch

enum class MainScreen(val route: String, val titleRes: Int, val icon: ImageVector) {
    Schedule("schedule", R.string.nav_schedule, Icons.Default.DateRange),
    Maintenance("maintenance", R.string.nav_maintenance, Icons.Default.Build),
    Today("today", R.string.nav_events, Icons.Default.Event),
    Profile("profile", R.string.profile, Icons.Default.AccountCircle)
}

@Composable
fun NavGraph(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val items = remember { MainScreen.values() }
    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearStatusMessage()
            }
        }
    }

    // Navigate to the Schedule screen automatically when the user logs out
    LaunchedEffect(isUserLoggedIn) {
        if (!isUserLoggedIn) {
            navController.navigate(MainScreen.Schedule.route) {
                // Clear the back stack so the Profile route (which now shows the Login UI) is removed
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Only show bottom bar for main screens
            if (items.any { it.route == currentRoute }) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationBarItem(
                            selected = currentDestination?.route == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    launchSingleTop = true
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(screen.titleRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainScreen.Schedule.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainScreen.Schedule.route) { HallScheduleScreen(viewModel) }
            composable(MainScreen.Maintenance.route) { MaintenanceJarScreen(viewModel) }
            composable(MainScreen.Today.route) { TodayEventsScreen(viewModel) }
            composable(MainScreen.Profile.route) {
                if (isUserLoggedIn) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onNavigateToBookings = { navController.navigate("my_bookings") },
                        onNavigateToRequests = { navController.navigate("my_requests") }
                    )
                } else {
                    LoginScreen(viewModel, onLoginSuccess = {
                        viewModel.refreshAuthState()
                        navController.navigate(MainScreen.Profile.route) {
                            popUpTo(MainScreen.Profile.route) { inclusive = true }
                        }
                    })
                }
            }
            composable("my_bookings") {
                MyBookingsScreen(viewModel, onBack = { navController.popBackStack() })
            }
            composable("my_requests") {
                MyMaintenanceRequestsScreen(viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
