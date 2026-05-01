package com.jstr14.picaday.domain.repository

import com.jstr14.picaday.domain.model.Album
import com.jstr14.picaday.domain.model.DayEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface AlbumRepository {
    /** Live stream of all albums the current user belongs to. */
    fun getAlbumsForUser(): Flow<List<Album>>

    /** Live stream of a single album by id. */
    fun getAlbum(albumId: String): Flow<Album?>

    fun getEntriesForMonth(albumId: String, month: YearMonth): Flow<List<DayEntry>>
    fun getEntriesForYear(albumId: String, year: Int): Flow<List<DayEntry>>

    suspend fun createAlbum(name: String): Album

    /**
     * Looks up the user by email in the top-level users collection and adds them as a member.
     * Returns Result<true> if the user was added immediately, Result<false> if they are not
     * registered yet and a pending invite was saved instead.
     */
    suspend fun inviteMemberByEmail(albumId: String, email: String): Result<Boolean>

    suspend fun removeMember(albumId: String, userId: String)

    suspend fun removePendingInvite(albumId: String, email: String)

    suspend fun renameAlbum(albumId: String, newName: String)

    suspend fun deleteAlbum(albumId: String)

    suspend fun deletePhotoFromAlbum(albumId: String, date: LocalDate, photoUrl: String)

    /** One-shot fetch of all entries across all dates in an album. */
    suspend fun getAllEntries(albumId: String): List<DayEntry>

    /** Live stream of all entries across all dates in an album. */
    fun getAllEntriesFlow(albumId: String): Flow<List<DayEntry>>

    /** Fetches photoUrl from users/{uid} for each given userId. Missing entries are omitted. */
    suspend fun getMemberPhotoUrls(memberIds: List<String>): Map<String, String>

    suspend fun addPhotoToAlbumEntry(
        albumId: String,
        date: LocalDate,
        imageUrl: String,
        time: String? = null,
        lat: Double? = null,
        lon: Double? = null,
    )
}
