package com.jstr14.picaday.data.model

import com.jstr14.picaday.domain.model.DayEntry
import java.time.LocalDate

fun DayEntryDto.toDomain(): DayEntry {
    return DayEntry(
        date = LocalDate.parse(this.date),
        imageUrls = this.imageUrls,
        description = this.description
    )
}

fun DayEntry.toDto(): DayEntryDto {
    val monthValue = date.monthValue.toString().padStart(2, '0')
    return DayEntryDto(
        date = this.date.toString(),              // "2026-04-15"
        yearMonth = "${date.year}-$monthValue",   // "2026-04" -> Used for the queries
        imageUrls = this.imageUrls,
        description = this.description
    )
}