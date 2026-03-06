package com.example.myliverecord.ui.navigation

sealed class Routes(val route: String) {
    data object History : Routes("history")
    data object AddLive : Routes("add_live")
}
