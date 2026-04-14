package com.jstr14.picaday.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jstr14.picaday.ui.calendar.components.CalendarDayCell
import com.jstr14.picaday.ui.calendar.components.CalendarHeader
import com.jstr14.picaday.ui.calendar.components.DaysOfWeekTitle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen() {
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

    val visibleMonth = remember(state.firstVisibleMonth) { state.firstVisibleMonth.yearMonth }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Static Header (Stays fixed while scrolling)
        CalendarHeader(visibleMonth = visibleMonth)

        // 2. Static days-of-week row (fixed, does not scroll with the calendar)
        DaysOfWeekTitle(daysOfWeek = daysOfWeek)

        // 3. The Calendar Grid
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                CalendarDayCell(day = day) { /* TODO: Navigate */ }
            }
        )
    }
}