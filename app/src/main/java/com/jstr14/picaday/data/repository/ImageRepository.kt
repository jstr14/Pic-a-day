package com.jstr14.picaday.domain.repository

import com.jstr14.picaday.domain.model.DayEntry
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * Interface for image data operations.
 */
interface ImageRepository {
    /**
     * Retrieves all entries for a specific month.
     */
    fun getEntriesForMonth(month: YearMonth): Flow<List<DayEntry>>
}