package com.shoron.mono.power

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/** Display-off and power-management helpers. No wake locks, no polling. */
object ScreenControl {

    /** True when MONO's accessibility service is connected and can turn the screen off. */
    fun canSleep(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && MonoAccessibilityService.instance != null

    /** Turns the display off like the power button. Returns false if not possible. */
    fun sleep(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val service = MonoAccessibilityService.instance ?: return false
        Diagnostics.count(Diagnostics.SCREEN_OFF_REQUESTS)
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val me = ComponentName(context, MonoAccessibilityService::class.java)
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == me
        }
    }

    fun openAccessibilitySettings(context: Context) = start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    /** Whether Android exempts MONO from battery optimisation (doze app-standby limits). */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    /**
     * Asks the user to exempt MONO from battery optimisation. A Home app that the OEM freezes
     * during long standby is the most likely reason the stock launcher flashes up on unlock.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        if (!start(context, request)) start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    /** App info page, where Realme keeps "Battery usage → Allow background activity". */
    fun openAppInfo(context: Context) = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
    )

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
