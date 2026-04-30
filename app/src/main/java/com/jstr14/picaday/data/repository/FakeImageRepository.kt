package com.jstr14.picaday.data.repository

import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.model.Photo
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
                    photos = listOf(
                        Photo(url = "https://picsum.photos/seed/${date.dayOfMonth}/200"),
                        Photo(url = "https://picsum.photos/seed/${date.dayOfMonth + 100}/200"),
                        Photo(url = "https://picsum.photos/seed/${date.dayOfMonth + 200}/200"),
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
                    photos = listOf(Photo(url = "https://picsum.photos/seed/${date.dayOfMonth}/200"))
                )
            }
        emit(entries)
    }

    override suspend fun saveDayEntry(dayEntry: DayEntry) {
        Unit
    }

    override suspend fun updatePhotos(date: LocalDate, photos: List<Photo>) {
        Unit
    }

    override suspend fun deleteDayEntry(date: LocalDate) {
        Unit
    }

    override suspend fun addPhotoToDate(date: LocalDate, imageUrl: String, time: String?, lat: Double?, lon: Double?) {
        Unit
    }

    override suspend fun updateDescription(date: LocalDate, description: String) {
        Unit
    }

    override suspend fun getEntry(date: LocalDate): DayEntry? = null
}