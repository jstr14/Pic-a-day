package com.jstr14.picaday.data.model

import com.jstr14.picaday.domain.model.Photo

data class PhotoDto(
    val url: String = "",
    val time: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
)

fun PhotoDto.toDomain() = Photo(url = url, time = time, lat = lat, lon = lon)
fun Photo.toDto() = PhotoDto(url = url, time = time, lat = lat, lon = lon)
