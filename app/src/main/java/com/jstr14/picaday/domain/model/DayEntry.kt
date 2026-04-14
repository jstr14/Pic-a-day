package com.jstr14.picaday.domain.model

import java.time.LocalDate

/**
 * Domain representation of a day containing images.
 */
data class DayEntry(
    val date: LocalDate,
    val imageUrls: List<String>,
    val totalCount: Int = imageUrls.size,
    val description: String? = null,
)