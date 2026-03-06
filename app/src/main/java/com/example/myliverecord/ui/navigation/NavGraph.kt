package com.example.myliverecord.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            )
        }
        composable(Routes.AddLive.route) {
            AddLiveScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
