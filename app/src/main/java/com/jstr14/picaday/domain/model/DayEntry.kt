package com.jstr14.picaday.domain.model

import java.time.LocalDate

/**
 * Domain representation of a day containing images.
 */
data class DayEntry(
    val date: LocalDate,
    val photos: List<Photo>,
    val description: String? = null,
) {
    val imageUrls: List<String> get() = photos.map { it.url }
}