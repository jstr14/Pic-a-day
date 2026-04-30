package com.jstr14.picaday.domain.model

data class Photo(
    val url: String,
    val time: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val albumId: String? = null,
    val albumNames: List<String> = emptyList(),
    val uploadedByUid: String? = null,
    val contributorName: String? = null,
)
