package com.shoron.mono.setup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.shoron.mono.media.MediaAccessService
import com.shoron.mono.power.MonoAccessibilityService
import com.shoron.mono.power.ScreenControl
import com.shoron.mono.settings.DefaultHome

/**
 * Status checks and Settings intents for each setup step. Everything here only *reads* state or
 * *opens* Android Settings — nothing is granted automatically, and no security is bypassed.
 */
object SetupChecks {

    fun isGranted(context: Context, id: SetupStepId, weatherConfigured: Boolean): Boolean = when (id) {
        SetupStepId.Home -> DefaultHome.isDefault(context)
        SetupStepId.Overlay -> Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(context)
        SetupStepId.Accessibility -> ScreenControl.isAccessibilityEnabled(context)
        SetupStepId.Notifications -> NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        SetupStepId.Battery -> ScreenControl.isIgnoringBatteryOptimizations(context)
        SetupStepId.Weather -> weatherConfigured
    }

    fun isRequired(id: SetupStepId): Boolean = id == SetupStepId.Home

    /** Opens the Android Settings screen for a step. Returns false if none could be opened. */
    fun open(context: Context, id: SetupStepId): Boolean = when (id) {
        SetupStepId.Home -> {
            DefaultHome.openSystemHomeSettings(context)
            true
        }
        SetupStepId.Overlay -> start(
            context,
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
        )
        SetupStepId.Accessibility -> openAccessibilityFor(context, ComponentName(context, MonoAccessibilityService::class.java))
        SetupStepId.Notifications -> openNotificationAccessFor(context, ComponentName(context, MediaAccessService::class.java))
        SetupStepId.Battery -> {
            ScreenControl.requestIgnoreBatteryOptimizations(context)
            true
        }
        SetupStepId.Weather -> false // handled inside MONO settings
    }

    /** App info for [packageName] — where "⋮ → Allow restricted settings" lives. */
    fun openAppInfo(context: Context, packageName: String): Boolean = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
    )

    /** Accessibility settings; on Android 11+ jumps to the service's own page when supported. */
    fun openAccessibilityFor(context: Context, service: ComponentName): Boolean {
        val detail = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
            .putExtra(Intent.EXTRA_COMPONENT_NAME, service.flattenToString())
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && start(context, detail)) ||
            start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /** Notification-access settings; on Android 11+ jumps to the listener's own page. */
    fun openNotificationAccessFor(context: Context, listener: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
            if (start(context, detail)) return true
        }
        return start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /**
     * Android 13+ blocks Accessibility and Notification-access toggles for apps installed from an
     * APK file ("Restricted setting") until the user allows it in App info. Apps installed by an
     * app store are not affected.
     */
    fun restrictedSettingsMayApply(context: Context, packageName: String = context.packageName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val installer = runCatching {
            context.packageManager.getInstallSourceInfo(packageName).installingPackageName
        }.getOrNull()
        return installer == null || installer !in STORE_INSTALLERS
    }

    private val STORE_INSTALLERS = setOf(
        "com.android.vending", // Google Play
        "com.heytap.market", // Realme/OPPO App Market
        "com.oppo.market",
        "com.sec.android.app.samsungapps",
        "com.huawei.appmarket",
        "com.xiaomi.market",
    )

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
