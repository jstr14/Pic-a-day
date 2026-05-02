package com.jstr14.picaday.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

class CyclePhotoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val count = prefs[WidgetKeys.IMAGE_COUNT] ?: 0
            val current = prefs[WidgetKeys.CURRENT_INDEX] ?: 0
            prefs.toMutablePreferences().apply {
                if (count > 0) this[WidgetKeys.CURRENT_INDEX] = (current + 1) % count
            }
        }
        MemoryWidget().update(context, glanceId)
    }
}
