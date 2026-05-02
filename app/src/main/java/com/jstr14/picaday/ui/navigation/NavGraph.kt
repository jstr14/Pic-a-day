package com.jstr14.picaday.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jstr14.picaday.ui.albums.AlbumDetailScreen
import com.jstr14.picaday.ui.auth.AuthViewModel
import com.jstr14.picaday.ui.daydetail.DayDetailScreen
import com.jstr14.picaday.ui.screens.HomeScreen
import com.jstr14.picaday.ui.screens.LoginScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    authViewModel: AuthViewModel,
    onGoogleSignIn: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onSignInClick = onGoogleSignIn
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                navController = navController,
            )
        }

        composable(
            route = Screen.DayDetail.route,
            arguments = listOf(navArgument("dayId") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("dayId") ?: ""
            val navigateBack = {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("scrollToDate", date)
                navController.popBackStack()
                Unit
            }
            BackHandler(onBack = navigateBack)
            DayDetailScreen(
                date = date,
                onBack = navigateBack
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) {
            AlbumDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}