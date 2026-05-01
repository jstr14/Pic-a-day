package com.jstr14.picaday.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.YearMonth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jstr14.picaday.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jstr14.picaday.ui.albums.AlbumsScreen
import com.jstr14.picaday.ui.auth.AuthViewModel
import com.jstr14.picaday.ui.calendar.CalendarScreen
import com.jstr14.picaday.ui.calendar.CalendarViewModel
import com.jstr14.picaday.ui.navigation.BottomNavBar
import com.jstr14.picaday.ui.navigation.BottomNavTab
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

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(BottomNavTab.HOME.ordinal) }
    val selectedTab = BottomNavTab.entries[selectedTabIndex]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            BottomNavTab.HOME -> stringResource(R.string.app_title)
                            BottomNavTab.ALBUMS -> stringResource(R.string.nav_albums)
                            BottomNavTab.FAVORITES -> stringResource(R.string.nav_favorites)
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (selectedTab == BottomNavTab.HOME) {
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
                                text = stringResource(R.string.today),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.cd_sign_out)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabIndex = it.ordinal }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                BottomNavTab.HOME -> CalendarScreen(
                    navController = navController,
                    viewModel = calendarViewModel
                )
                BottomNavTab.ALBUMS -> AlbumsScreen(
                    onAlbumClick = { albumId ->
                        navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                    }
                )
                BottomNavTab.FAVORITES -> FavoritesPlaceholder()
            }
        }
    }
}

@Composable
private fun FavoritesPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.nav_favorites),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
