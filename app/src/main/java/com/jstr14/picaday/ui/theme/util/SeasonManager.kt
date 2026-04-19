package com.jstr14.picaday.ui.theme.util

import java.time.LocalDate

enum class Season { SPRING, SUMMER, AUTUMN, WINTER }

object SeasonManager {
    // CAMBIA ESTO PARA TESTEAR: null = fecha real | LocalDate.of(2026, 12, 25) = Navidad
    var mockDate: LocalDate? = null

    private fun getToday(): LocalDate = mockDate ?: LocalDate.now()

    enum class SpecialDay { NONE, HALLOWEEN, CHRISTMAS, VALENTINE, NEW_YEAR }

    fun getCurrentSeason(): Season {
        val date = getToday()
        return when (date.monthValue) {
            in 3..5 -> Season.SPRING
            in 6..8 -> Season.SUMMER
            in 9..11 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }

    fun getSpecialDay(): SpecialDay {
        val date = getToday()
        val month = date.monthValue
        val day = date.dayOfMonth

        return when {
            month == 10 && day >= 25 -> SpecialDay.HALLOWEEN
            month == 12 && day >= 20 -> SpecialDay.CHRISTMAS
            month == 1 && day <= 5 -> SpecialDay.NEW_YEAR
            month == 2 && day == 14 -> SpecialDay.VALENTINE
            else -> SpecialDay.NONE
        }
    }
}