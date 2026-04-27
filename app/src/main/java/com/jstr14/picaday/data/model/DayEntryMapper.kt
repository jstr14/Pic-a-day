package com.jstr14.picaday.data.model

import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.model.Photo
import java.time.LocalDate

fun DayEntryDto.toDomain(): DayEntry {
    // Prefer the new `photos` list; fall back to legacy `imageUrls` for old Firestore documents
    val resolvedPhotos = if (photos.isNotEmpty()) {
        photos.map { it.toDomain() }
    } else {
        imageUrls.map { Photo(url = it) }
    }
    return DayEntry(
        date = LocalDate.parse(this.date),
        photos = resolvedPhotos,
        description = this.description,
    )
}

fun DayEntry.toDto(): DayEntryDto {
    val monthValue = date.monthValue.toString().padStart(2, '0')
    return DayEntryDto(
        date = this.date.toString(),              // "2026-04-15"
        yearMonth = "${date.year}-$monthValue",   // "2026-04" -> Used for the queries
        photos = this.photos.map { it.toDto() },
        description = this.description,
    )
}