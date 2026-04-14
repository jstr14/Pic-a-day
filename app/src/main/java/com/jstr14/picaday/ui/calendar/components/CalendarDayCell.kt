package com.jstr14.picaday.ui.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    images: List<String>,
    onDayClick: (CalendarDay) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onDayClick(day) }
            )
    ) {
        if (day.position == DayPosition.MonthDate) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ImageStack(imageUrls = images)
                }
            }
        }
    }
}