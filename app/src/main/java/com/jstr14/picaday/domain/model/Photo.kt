package com.jstr14.picaday.domain.model

data class Photo(
    val url: String,
    val time: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
)
