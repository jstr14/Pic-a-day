package com.jstr14.picaday.widget

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object WidgetKeys {
    val STATUS = stringPreferencesKey("widget_status")
    val IMAGE_PATHS = stringSetPreferencesKey("widget_image_paths")
    val CURRENT_INDEX = intPreferencesKey("widget_current_index")
    val DATE_LABEL = stringPreferencesKey("widget_date_label")
    val IMAGE_COUNT = intPreferencesKey("widget_image_count")
    val FOUND_DATE = stringPreferencesKey("widget_found_date")
}

const val EXTRA_WIDGET_DATE = "widget_date"

object WidgetStatus {
    const val LOADING = "loading"
    const val NOT_LOGGED_IN = "not_logged_in"
    const val NO_MEMORIES = "no_memories"
    const val READY = "ready"
}
