package com.jstr14.picaday.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Home : Screen("home_screen")
}