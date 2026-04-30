package com.jstr14.picaday.data.model

import com.jstr14.picaday.domain.model.Album
import com.jstr14.picaday.domain.model.AlbumMember
import com.jstr14.picaday.domain.model.AlbumRole

data class AlbumDto(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    // userId -> role string ("owner" | "admin" | "member")
    val members: Map<String, String> = emptyMap(),
    // denormalized for display without extra reads
    val memberEmails: Map<String, String> = emptyMap(),
    val memberDisplayNames: Map<String, String> = emptyMap(),
    val memberPhotoUrls: Map<String, String> = emptyMap(),
)

fun AlbumDto.toDomain() = Album(
    id = id,
    name = name,
    ownerId = ownerId,
    members = members.map { (userId, role) ->
        AlbumMember(
            userId = userId,
            role = when (role) {
                "owner" -> AlbumRole.OWNER
                "admin" -> AlbumRole.ADMIN
                else -> AlbumRole.MEMBER
            },
            email = memberEmails[userId],
            displayName = memberDisplayNames[userId],
            photoUrl = memberPhotoUrls[userId],
        )
    },
)
