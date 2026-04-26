package com.jstr14.picaday.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun YearOverviewCalendar(
    year: Int,
    entryDates: Set<LocalDate>,
    firstDayOfWeek: DayOfWeek,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(12) { monthIndex ->
            val yearMonth = YearMonth.of(year, monthIndex + 1)
            MiniMonthCell(
                yearMonth = yearMonth,
                entryDates = entryDates,
                firstDayOfWeek = firstDayOfWeek,
                onClick = { onMonthClick(yearMonth) }
            )
        }
    }
}

@Composable
private fun MiniMonthCell(
    yearMonth: YearMonth,
    entryDates: Set<LocalDate>,
    firstDayOfWeek: DayOfWeek,
    onClick: () -> Unit
) {
    val monthName = yearMonth.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOffset = (yearMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val totalCells = firstDayOffset + daysInMonth
    val weeks = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = monthName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        for (week in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dow in 0 until 7) {
                    val dayNum = week * 7 + dow - firstDayOffset + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNum)
                            val hasEntry = date in entryDates
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.85f)
                                    .clip(CircleShape)
                                    .then(
                                        if (hasEntry) Modifier.background(MaterialTheme.colorScheme.primary)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 8.sp,
                                    fontWeight = if (hasEntry) FontWeight.Bold else FontWeight.Normal,
                                    color = if (hasEntry) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
