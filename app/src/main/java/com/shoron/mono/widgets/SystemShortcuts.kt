package com.shoron.mono.widgets

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract

/** Opens the user's own clock, calendar and battery screens from widget taps. */
object SystemShortcuts {

    fun openAlarms(context: Context): Boolean = start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openCalendar(context: Context): Boolean {
        val app = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        if (start(context, app)) return true
        val today = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").let {
            ContentUris.appendId(it, System.currentTimeMillis())
        }.build()
        return start(context, Intent(Intent.ACTION_VIEW, today))
    }

    fun openBatteryUsage(context: Context): Boolean = start(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
