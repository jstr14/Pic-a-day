package com.jstr14.picaday.data.repository

import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.model.Photo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * Interface for image data operations.
 */
interface ImageRepository {
    /**
     * Retrieves all entries for a specific month.
     */
    fun getEntriesForMonth(month: YearMonth): Flow<List<DayEntry>>

    /**
     * Retrieves all entries for a specific year.
     */
    fun getEntriesForYear(year: Int): Flow<List<DayEntry>>
    /**
     * Save day entry in Firebase.
     */
    suspend fun saveDayEntry(dayEntry: DayEntry)

    suspend fun updatePhotos(date: LocalDate, photos: List<Photo>)

    suspend fun deleteDayEntry(date: LocalDate)

    suspend fun addPhotoToDate(date: LocalDate, imageUrl: String, time: String? = null, lat: Double? = null, lon: Double? = null)

    suspend fun getEntry(date: LocalDate): DayEntry?

    suspend fun updateDescription(date: LocalDate, description: String)
}