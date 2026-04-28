package com.jstr14.picaday.domain.model

import java.time.LocalDate

data class ProcessedImage(
    val bytes: ByteArray,
    val date: LocalDate,
    val time: String? = null,  // "HH:mm" from EXIF, null if not available
    val lat: Double? = null,
    val lon: Double? = null,
)