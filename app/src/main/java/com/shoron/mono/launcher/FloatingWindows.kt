package com.shoron.mono.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle

/**
 * Floating (freeform) windows through public APIs only.
 *
 * The Realme "hold the Home gesture and drag to a corner" interaction is handled by SystemUI
 * and Realme's own recents launcher; Android does not deliver navigation gestures to third-party
 * launchers, so MONO cannot detect or reproduce it. The supported alternative is asking Android
 * to launch an app with freeform launch bounds — honoured only on devices that declare
 * FEATURE_FREEFORM_WINDOW_MANAGEMENT. MONO only offers it there, and words it as a request.
 */
object FloatingWindows {

    fun isSupported(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)

    /** Compact centred window: 80% width, 50% height of the display. */
    fun options(context: Context): Bundle {
        val m = context.resources.displayMetrics
        val bounds = FloatingBounds.compact(m.widthPixels, m.heightPixels)
        return ActivityOptions.makeBasic().setLaunchBounds(bounds).toBundle()
    }
}

/** Pure bounds maths (JVM-tested values; android.graphics.Rect only at the edge). */
object FloatingBounds {
    fun compactValues(screenW: Int, screenH: Int): IntArray {
        val w = (screenW * 0.8f).toInt()
        val h = (screenH * 0.5f).toInt()
        val left = (screenW - w) / 2
        val top = (screenH - h) / 3
        return intArrayOf(left, top, left + w, top + h)
    }

    fun compact(screenW: Int, screenH: Int): Rect = compactValues(screenW, screenH).let { Rect(it[0], it[1], it[2], it[3]) }
}
