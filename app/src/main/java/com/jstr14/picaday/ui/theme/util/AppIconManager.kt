package com.jstr14.picaday.ui.theme.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconManager {

    private val aliasMap = mapOf(
        "spring"    to "com.jstr14.picaday.MainActivitySpring",
        "summer"    to "com.jstr14.picaday.MainActivitySummer",
        "autumn"    to "com.jstr14.picaday.MainActivityAutumn",
        "winter"    to "com.jstr14.picaday.MainActivityWinter",
        "halloween" to "com.jstr14.picaday.MainActivityHalloween",
        "christmas" to "com.jstr14.picaday.MainActivityChristmas",
        "valentine" to "com.jstr14.picaday.MainActivityValentine",
        "newyear"   to "com.jstr14.picaday.MainActivityNewYear"
    )

    private fun currentKey(): String {
        return when (SeasonManager.getSpecialDay()) {
            SeasonManager.SpecialDay.HALLOWEEN -> "halloween"
            SeasonManager.SpecialDay.CHRISTMAS -> "christmas"
            SeasonManager.SpecialDay.VALENTINE -> "valentine"
            SeasonManager.SpecialDay.NEW_YEAR  -> "newyear"
            SeasonManager.SpecialDay.NONE -> when (SeasonManager.getCurrentSeason()) {
                Season.SPRING -> "spring"
                Season.SUMMER -> "summer"
                Season.AUTUMN -> "autumn"
                Season.WINTER -> "winter"
            }
        }
    }

    fun update(context: Context) {
        val pm = context.packageManager
        val targetKey = currentKey()
        aliasMap.forEach { (key, className) ->
            val state = if (key == targetKey) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(context.packageName, className),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) { }
        }
    }
}
