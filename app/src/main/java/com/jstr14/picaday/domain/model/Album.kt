package com.jstr14.picaday.domain.model

data class Album(
    val id: String,
    val name: String,
    val ownerId: String,
    val members: List<AlbumMember>,
)

data class AlbumMember(
    val userId: String,
    val role: AlbumRole,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
)

enum class AlbumRole { OWNER, ADMIN, MEMBER }
