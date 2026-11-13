package com.shoron.mono.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager

/** Starts apps and system screens on behalf of the launcher. */
class AppLauncher(context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    fun launch(entry: AppEntry): Boolean = runCatching {
        val user = userManager.getUserForSerialNumber(entry.userSerial) ?: return false
        launcherApps.startMainActivity(ComponentName(entry.packageName, entry.className), user, null, null)
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
