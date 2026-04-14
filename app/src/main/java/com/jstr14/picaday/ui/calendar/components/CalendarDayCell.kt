package com.jstr14.picaday.ui.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jstr14.picaday.ui.components.ImageStack
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition

/**
 * Custom cell component for each calendar day.
 * It displays the [ImageStack] if the day belongs to the current month.
 */
@Composable
fun CalendarDayCell(
    day: CalendarDay,
    onDayClick: (CalendarDay) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Maintains a square shape for the cell
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onDayClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Only render content for days that belong to the current month
        if (day.position == DayPosition.MonthDate) {

            // ImageStack integration
            // TODO: Pass actual image URLs from ViewModel in the next step
            ImageStack(
                imageUrls = emptyList(),
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )

            // Day number overlay
            Text(
                text = day.date.dayOfMonth.toString(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(4.dp)
            )
        }
    }
}