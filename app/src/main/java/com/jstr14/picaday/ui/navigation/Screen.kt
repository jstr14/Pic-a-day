package com.jstr14.picaday.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Home : Screen("home_screen")
    object DayDetail : Screen("day_detail_screen/{dayId}") {
        fun createRoute(date: String) = "day_detail_screen/$date"
    }
    object Albums : Screen("albums_screen")
    object AlbumDetail : Screen("album_detail_screen/{albumId}") {
        fun createRoute(albumId: String) = "album_detail_screen/$albumId"
    }
    object Favorites : Screen("favorites_screen")
}