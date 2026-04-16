package com.jstr14.picaday.domain.model

import java.time.LocalDate

data class ProcessedImage(
    val bytes: ByteArray,
    val date: LocalDate
)