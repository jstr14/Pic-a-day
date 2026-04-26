package com.jstr14.picaday.data.repository

import com.jstr14.picaday.domain.model.DayEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Fake implementation for testing the Calendar UI.
 */
class FakeImageRepository @Inject constructor() : ImageRepository {
    override fun getEntriesForMonth(month: YearMonth): Flow<List<DayEntry>> = flow {
        val entries = mutableListOf<DayEntry>()
        val today = LocalDate.now()

        // Let's generate some fake data for the last 10 days
        for (i in 0..10) {
            val date = today.minusDays(i.toLong())
            entries.add(
                DayEntry(
                    date = date,
                    imageUrls = listOf(
                        "https://picsum.photos/seed/${date.dayOfMonth}/200",
                        "https://picsum.photos/seed/${date.dayOfMonth + 100}/200",
                        "https://picsum.photos/seed/${date.dayOfMonth + 200}/200"
                    )
                )
            )
        }
        emit(entries)
    }

    override fun getEntriesForYear(year: Int): Flow<List<DayEntry>> = flow {
        val today = LocalDate.now()
        val entries = (0..60).map { i -> today.minusDays(i.toLong()) }
            .filter { it.year == year }
            .map { date ->
                DayEntry(
                    date = date,
                    imageUrls = listOf("https://picsum.photos/seed/${date.dayOfMonth}/200")
                )
            }
        emit(entries)
    }

    override suspend fun saveDayEntry(dayEntry: DayEntry) {
        Unit
    }

    override suspend fun updateImageUrls(
        date: LocalDate,
        newUrls: List<String>
    ) {
        Unit
    }

    override suspend fun deleteDayEntry(date: LocalDate) {
        Unit
    }

    override suspend fun addPhotoToDate(date: LocalDate, imageUrl: String) {
        Unit
    }
}