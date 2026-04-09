package com.jstr14.picaday.domain.model

data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)