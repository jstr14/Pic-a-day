package com.jstr14.picaday.ui.calendar

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

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
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
                val entry = entries.find { it.date == day.date }

                CalendarDayCell(
                    day = day,
                    images = entry?.imageUrls ?: emptyList() // Pass the actual URLs!
                ) { /* Handle click */ }
            },
            monthHeader = { DaysOfWeekTitle(daysOfWeek = daysOfWeek) }
        )
    }
}