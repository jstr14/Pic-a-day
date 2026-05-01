package com.jstr14.picaday.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.YearMonth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import java.util.Locale
import com.jstr14.picaday.R
import com.jstr14.picaday.ui.components.CircularAvatar
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jstr14.picaday.domain.model.User
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
    val yearModeYear by calendarViewModel.yearModeYear.collectAsState()
    val isCurrentMonth = visibleMonth == YearMonth.now() && !isYearMode
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    var showProfileSheet by remember { mutableStateOf(false) }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(BottomNavTab.HOME.ordinal) }
    val selectedTab = BottomNavTab.entries[selectedTabIndex]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (selectedTab == BottomNavTab.HOME) {
                        val monthName = visibleMonth.month
                            .getDisplayName(TextStyle.FULL, Locale.getDefault())
                            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!isYearMode) calendarViewModel.setYearModeYear(visibleMonth.year)
                                    calendarViewModel.setYearMode(!isYearMode)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isYearMode) yearModeYear.toString() else "$monthName ${visibleMonth.year}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = if (isYearMode) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isYearMode) stringResource(R.string.cd_back_to_month) else stringResource(R.string.cd_show_year),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Text(
                            when (selectedTab) {
                                BottomNavTab.ALBUMS -> stringResource(R.string.nav_albums)
                                BottomNavTab.FAVORITES -> stringResource(R.string.nav_favorites)
                                else -> ""
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (selectedTab == BottomNavTab.HOME) {
                        IconButton(onClick = {
                            if (isYearMode) calendarViewModel.decrementYear()
                            else calendarViewModel.requestScrollToPreviousMonth()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_previous_month))
                        }
                        Box(
                            modifier = Modifier
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
                        IconButton(onClick = {
                            if (isYearMode) calendarViewModel.incrementYear()
                            else calendarViewModel.requestScrollToNextMonth()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next_month))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { showProfileSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(user = currentUser, size = 32.dp)
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
        if (showProfileSheet) {
            ModalBottomSheet(onDismissRequest = { showProfileSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileAvatar(user = currentUser, size = 72.dp)
                    Spacer(Modifier.height(16.dp))
                    currentUser?.displayName?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    currentUser?.email?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { authViewModel.signOut(); showProfileSheet = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_out))
                    }
                }
            }
        }

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
private fun ProfileAvatar(user: User?, size: Dp) {
    CircularAvatar(
        photoUrl = user?.photoUrl,
        size = size,
        contentDescription = stringResource(R.string.cd_profile)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
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
