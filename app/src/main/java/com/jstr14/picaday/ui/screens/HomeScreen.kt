package com.jstr14.picaday.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import java.time.YearMonth
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jstr14.picaday.ui.auth.AuthViewModel
import com.jstr14.picaday.ui.calendar.CalendarScreen
import com.jstr14.picaday.ui.calendar.CalendarViewModel
import com.jstr14.picaday.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    navController: NavHostController,
    calendarViewModel: CalendarViewModel = hiltViewModel()
) {
    val visibleMonth by calendarViewModel.visibleMonth.collectAsState()
    val isYearMode by calendarViewModel.isYearMode.collectAsState()
    val isCurrentMonth = visibleMonth == YearMonth.now() && !isYearMode

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("PicADay") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            .clickable(enabled = !isCurrentMonth) { calendarViewModel.requestScrollToToday() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Albums.route) }) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = "Álbumes compartidos"
                        )
                    }
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            CalendarScreen(navController = navController, viewModel = calendarViewModel)
        }
    }
}