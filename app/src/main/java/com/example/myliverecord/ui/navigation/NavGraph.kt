package com.example.myliverecord.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myliverecord.ui.screens.add.AddLiveScreen
import com.example.myliverecord.ui.screens.history.HistoryScreen
import com.example.myliverecord.ui.screens.summary.YearSummaryScreen

private val bottomNavRoutes = setOf(Routes.History.route, Routes.YearSummary.route)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.History.route,
                        onClick = {
                            navController.navigate(Routes.History.route) {
                                popUpTo(Routes.History.route) { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("履歴") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.YearSummary.route,
                        onClick = {
                            navController.navigate(Routes.YearSummary.route) {
                                popUpTo(Routes.History.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("年別集計") },
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.History.route,
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
        ) {
            composable(Routes.History.route) {
                HistoryScreen(
                    onNavigateToAdd = { navController.navigate(Routes.AddLive.route) },
                    onNavigateToEdit = { id -> navController.navigate(Routes.EditLive.createRoute(id)) },
                )
            }
            composable(Routes.YearSummary.route) {
                YearSummaryScreen()
            }
            composable(Routes.AddLive.route) {
                AddLiveScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.EditLive.route,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
            ) {
                AddLiveScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
