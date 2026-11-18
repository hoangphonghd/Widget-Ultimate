package com.magicos.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.concurrent.thread

class Tick : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val force = i.action == Render.ACTION_REFRESH
        val p = goAsync()
        thread { try { Render.updateAll(c, force) } finally { p.finish() } }
    }
}

open class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        val p = goAsync()
        thread { try { Render.updateAll(c, false) } finally { p.finish() } }
    }
}

class StdWidget : BaseWidget()
class GlassWidget : BaseWidget()
