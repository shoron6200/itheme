package com.shoron.mono.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import android.os.UserManager

/** Starts apps and system screens on behalf of the launcher. */
class AppLauncher(context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    /**
     * @param sourceBounds on-screen bounds of the tapped icon/row (window coordinates), used by
     *   the system for the launch transition.
     * @param options e.g. ActivityOptions.makeClipRevealAnimation(...).toBundle().
     */
    fun launch(entry: AppEntry, sourceBounds: Rect? = null, options: Bundle? = null): Boolean = runCatching {
        val user = userManager.getUserForSerialNumber(entry.userSerial) ?: return false
        launcherApps.startMainActivity(ComponentName(entry.packageName, entry.className), user, sourceBounds, options)
    }.isSuccess

    fun openAppInfo(entry: AppEntry): Boolean = runCatching {
        val user = userManager.getUserForSerialNumber(entry.userSerial) ?: return false
        launcherApps.startAppDetailsActivity(
            ComponentName(entry.packageName, entry.className),
            user,
            null,
            null,
        )
    }.isSuccess
}
