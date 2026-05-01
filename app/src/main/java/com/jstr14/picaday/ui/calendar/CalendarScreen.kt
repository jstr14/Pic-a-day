package com.jstr14.picaday.ui.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jstr14.picaday.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jstr14.picaday.ui.calendar.components.CalendarDayCell
import com.jstr14.picaday.ui.components.AlbumSelectorSheet
import com.jstr14.picaday.ui.components.StatusPillIndicator
import com.jstr14.picaday.ui.calendar.components.DaysOfWeekTitle
import com.jstr14.picaday.ui.calendar.components.YearOverviewCalendar
import com.jstr14.picaday.ui.navigation.Screen
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val today = remember { LocalDate.now() }
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(120) }
    val endMonth = remember { currentMonth.plusMonths(120) }
    val daysOfWeek = remember { daysOfWeek() }
    val scope = rememberCoroutineScope()

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    val entries by viewModel.entries.collectAsState()
    val visibleMonth = remember(state.firstVisibleMonth) { state.firstVisibleMonth.yearMonth }
    val isUploading by viewModel.isUploading.collectAsState()
    val yearEntryDates by viewModel.yearEntryDates.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isYearMode by viewModel.isYearMode.collectAsState()
    val yearModeYear by viewModel.yearModeYear.collectAsState()
    var showAlbumSelector by remember { mutableStateOf(false) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isYearMode, yearModeYear) {
        if (isYearMode) viewModel.loadYearData(yearModeYear)
    }

    LaunchedEffect(Unit) {
        viewModel.scrollToToday.collect {
            viewModel.setYearMode(false)
            state.animateScrollToMonth(currentMonth)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollToPreviousMonth.collect {
            state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollToNextMonth.collect {
            state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
        }
    }

    val context = LocalContext.current

    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.uploadMultipleImages(uris, selectedAlbumId)
        selectedAlbumId = null
    }

    val mediaLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchPickerWithLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            mediaLocationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun onDestinationSelected(albumId: String?) {
        selectedAlbumId = albumId
        showAlbumSelector = false
        launchPickerWithLocationPermission()
    }

    fun onUploadClick() {
        if (albums.isEmpty()) onDestinationSelected(null)
        else showAlbumSelector = true
    }

    if (showAlbumSelector) {
        AlbumSelectorSheet(
            albums = albums,
            onSelectPersonal = { onDestinationSelected(null) },
            onSelectAlbum = { album -> onDestinationSelected(album.id) },
            onDismiss = { showAlbumSelector = false }
        )
    }

    LaunchedEffect(visibleMonth) {
        viewModel.loadMonthData(visibleMonth)
        viewModel.updateVisibleMonth(visibleMonth)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!isYearMode) {
                FloatingActionButton(
                    onClick = { onUploadClick() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_upload_photo))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Calendar - Main content
            Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
                AnimatedVisibility(
                    visible = !isYearMode,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    HorizontalCalendar(
                        state = state,
                        dayContent = { day ->
                            val entryForDay = entries.find { it.date == day.date }
                            CalendarDayCell(
                                day = day,
                                images = entryForDay?.imageUrls ?: emptyList(),
                                isToday = day.date == today
                            ) { clickedDay ->
                                navController.navigate(Screen.DayDetail.createRoute(clickedDay.date.toString()))
                            }
                        },
                        monthHeader = { DaysOfWeekTitle(daysOfWeek = daysOfWeek) }
                    )
                }

                AnimatedVisibility(
                    visible = isYearMode,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    YearOverviewCalendar(
                        year = yearModeYear,
                        entryDates = yearEntryDates,
                        firstDayOfWeek = daysOfWeek.first(),
                        onMonthClick = { yearMonth ->
                            viewModel.setYearMode(false)
                            scope.launch { state.animateScrollToMonth(yearMonth) }
                        }
                    )
                }
            }

            // Loading indicator
            StatusPillIndicator(
                visible = isUploading,
                text = stringResource(R.string.saving_memories),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}