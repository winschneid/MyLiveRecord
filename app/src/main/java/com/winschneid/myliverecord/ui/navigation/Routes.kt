package com.winschneid.myliverecord.ui.navigation

import android.net.Uri

sealed class Routes(val route: String) {
    data object History : Routes("history")
    data object YearSummary : Routes("year_summary")
    data object AddLive : Routes("add_live")
    data object Settings : Routes("settings")
    data object EditLive : Routes("edit_live/{recordId}") {
        fun createRoute(recordId: Long) = "edit_live/$recordId"
    }
    data object ArtistDetail : Routes("artist_detail/{artistName}") {
        fun createRoute(artistName: String) = "artist_detail/${Uri.encode(artistName)}"
    }
}
