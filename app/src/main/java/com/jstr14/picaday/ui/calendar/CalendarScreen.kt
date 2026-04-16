package com.jstr14.picaday.ui.calendar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jstr14.picaday.ui.calendar.components.CalendarDayCell
import com.jstr14.picaday.ui.calendar.components.CalendarHeader
import com.jstr14.picaday.ui.calendar.components.DaysOfWeekTitle
import com.jstr14.picaday.ui.navigation.Screen
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.YearMonth

@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(120) }
    val endMonth = remember { currentMonth.plusMonths(120) }
    val daysOfWeek = remember { daysOfWeek() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    val entries by viewModel.entries.collectAsState()
    val visibleMonth = remember(state.firstVisibleMonth) { state.firstVisibleMonth.yearMonth }
    val isUploading by viewModel.isUploading.collectAsState()

    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadMultipleImages(uris)
        }
    }

    LaunchedEffect(visibleMonth) {
        viewModel.loadMonthData(visibleMonth)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Subir foto")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Contenido principal (Calendario)
            Column(modifier = Modifier.fillMaxSize()) {
                CalendarHeader(visibleMonth = visibleMonth)

                HorizontalCalendar(
                    state = state,
                    dayContent = { day ->
                        val entryForDay = entries.find { it.date == day.date }
                        CalendarDayCell(
                            day = day,
                            images = entryForDay?.imageUrls ?: emptyList()
                        ) { clickedDay ->
                            navController.navigate(Screen.DayDetail.createRoute(clickedDay.date.toString()))
                        }
                    },
                    monthHeader = { DaysOfWeekTitle(daysOfWeek = daysOfWeek) }
                )
            }

            // 2. Indicador de carga flotante con animación
            AnimatedVisibility(
                visible = isUploading,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 6.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Guardando tus recuerdos...",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}