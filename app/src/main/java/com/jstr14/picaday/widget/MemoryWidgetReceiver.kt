package com.jstr14.picaday.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MemoryWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MemoryWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MemoryWidgetWorker.schedulePeriodic(context)
    }
}
