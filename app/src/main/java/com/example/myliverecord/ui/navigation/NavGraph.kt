package com.example.myliverecord.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myliverecord.ui.screens.add.AddLiveScreen
import com.example.myliverecord.ui.screens.history.HistoryScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.History.route,
    ) {
        composable(Routes.History.route) {
            HistoryScreen(
                onNavigateToAdd = { navController.navigate(Routes.AddLive.route) },
                onNavigateToEdit = { id -> navController.navigate(Routes.EditLive.createRoute(id)) },
            )
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
