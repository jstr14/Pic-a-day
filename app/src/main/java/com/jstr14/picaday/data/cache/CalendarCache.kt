package com.jstr14.picaday.data.cache

import com.jstr14.picaday.domain.model.DayEntry
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap
class CalendarCache {

    private data class CacheEntry(
        val data: List<DayEntry>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isValid(): Boolean =
            System.currentTimeMillis() - timestamp < TTL_MS
    }

    private val cache = ConcurrentHashMap<YearMonth, CacheEntry>()

    fun get(month: YearMonth): List<DayEntry>? {
        val entry = cache[month] ?: return null
        return if (entry.isValid()) entry.data else null
    }

    fun put(month: YearMonth, data: List<DayEntry>) {
        cache[month] = CacheEntry(data)
    }

    fun invalidate(month: YearMonth) {
        cache.remove(month)
    }

    fun clear() {
        cache.clear()
    }

    fun allMonthsCached(year: Int): Boolean =
        (1..12).all { month -> get(YearMonth.of(year, month)) != null }

    fun getYear(year: Int): List<DayEntry> =
        (1..12).flatMap { month -> get(YearMonth.of(year, month)) ?: emptyList() }

    companion object {
        private const val TTL_MS = 5 * 60 * 1000L // 5 minutes
    }
}
