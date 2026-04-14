package com.jstr14.picaday.ui.calendar

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.jstr14.picaday.ui.calendar.components.CalendarDayCell
import com.jstr14.picaday.ui.calendar.components.CalendarHeader
import com.jstr14.picaday.ui.calendar.components.DaysOfWeekTitle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.jstr14.picaday.ui.navigation.Screen
import java.util.Map.entry

@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Range of 10 years from now and before now
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(120) }
    val endMonth = remember { currentMonth.plusMonths(120) }
    val selections = remember { mutableStateListOf<LocalDate>() }

    val daysOfWeek = remember { daysOfWeek() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    // Sync entries from ViewModel
    val entries by viewModel.entries.collectAsState()
    val visibleMonth = remember(state.firstVisibleMonth) { state.firstVisibleMonth.yearMonth }

    // Load data when the visible month changes
    LaunchedEffect(visibleMonth) {
        viewModel.loadMonthData(visibleMonth)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Static Header (Stays fixed while scrolling)
        CalendarHeader(visibleMonth = visibleMonth)

        // 2. The Calendar Grid
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                // Find the entry for this specific day
                val entryForDay = entries.find { it.date == day.date }
                val hasData = entryForDay != null && entryForDay.imageUrls.isNotEmpty()

                CalendarDayCell(
                    day = day,
                    images = entryForDay?.imageUrls ?: emptyList()
                ) { clickedDay ->
                    if (hasData) {
                        navController.navigate(Screen.DayDetail.createRoute(clickedDay.date.toString()))
                    } else {
                        Toast.makeText(
                            context,
                            "No hay fotos para este día aún",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            monthHeader = { DaysOfWeekTitle(daysOfWeek = daysOfWeek) }
        )
    }
}