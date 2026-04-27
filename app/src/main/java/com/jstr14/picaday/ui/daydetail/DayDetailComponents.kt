package com.jstr14.picaday.ui.daydetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
internal fun BottomAction(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = tint, fontSize = 11.sp)
    }
}

fun String.toPrettyDate(): String {
    return try {
        val localDate = LocalDate.parse(this)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        localDate.format(formatter)
    } catch (e: Exception) {
        this
    }
}

fun String.toShortDate(): String {
    return try {
        val localDate = LocalDate.parse(this)
        val month = localDate.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            .lowercase(Locale.getDefault()).trimEnd('.')
        "${localDate.dayOfMonth} $month"
    } catch (e: Exception) {
        this
    }
}

fun String.toLongDate(): String {
    return try {
        val localDate = LocalDate.parse(this)
        val dow = localDate.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            .lowercase(Locale.getDefault()).trimEnd('.')
        val month = localDate.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            .lowercase(Locale.getDefault()).trimEnd('.')
        "$dow, ${localDate.dayOfMonth} $month ${localDate.year}"
    } catch (e: Exception) {
        this
    }
}
