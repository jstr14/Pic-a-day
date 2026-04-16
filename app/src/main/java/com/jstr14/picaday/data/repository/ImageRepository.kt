package com.jstr14.picaday.data.repository

import com.jstr14.picaday.domain.model.DayEntry
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
     * Save day entry in Firebase.
     */
    suspend fun saveDayEntry(dayEntry: DayEntry)

    suspend fun updateImageUrls(date: LocalDate, newUrls: List<String>)

    suspend fun deleteDayEntry(date: LocalDate)

    suspend fun addPhotoToDate(date: LocalDate, imageUrl: String)
}