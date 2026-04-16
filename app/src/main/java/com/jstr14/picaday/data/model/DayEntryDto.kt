package com.jstr14.picaday.data.model

data class DayEntryDto(
    val date: String = "",
    val yearMonth: String = "",
    val imageUrls: List<String> = emptyList(),
    val description: String? = null
)