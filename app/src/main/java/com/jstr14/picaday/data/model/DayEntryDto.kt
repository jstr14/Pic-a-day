package com.jstr14.picaday.data.model

data class DayEntryDto(
    val date: String = "",
    val yearMonth: String = "",
    val photos: List<PhotoDto> = emptyList(),
    val imageUrls: List<String> = emptyList(), // legacy field for backwards compatibility
    val description: String? = null,
)