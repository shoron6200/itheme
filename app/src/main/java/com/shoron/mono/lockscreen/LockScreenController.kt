package com.shoron.mono.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.shoron.mono.data.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Shows the MONO lock screen when the display turns off, so it is already on top of Android's
 * keyguard when the user wakes the phone.
 *
 * Security model: MONO never dismisses or disables the keyguard. Its activity is merely
 * shown *over* the locked keyguard (showWhenLocked); unlocking always goes through the system's
 * own PIN / pattern / password / biometric UI via KeyguardManager.requestDismissKeyguard.
 *
 * Limits (Android, no root):
 *  - SCREEN_OFF can only be received by a running process; MONO's process normally stays alive
 *    because it is the Home app, but if the system kills it the glance is skipped until MONO runs
 *    again. No foreground service is used.
 *  - Starting an activity from the background needs "Display over other apps" on Android 10+;
 *    without it the glance only appears when MONO's home screen was in front.
 */
class LockScreenController(
    context: Context,
    private val preferences: WidgetPreferences,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) show()
        }
    }

    fun start() {
        scope.launch {
            preferences.lock.map { it.enabled }.distinctUntilChanged().collect { enabled ->
                if (enabled) register() else unregister()
            }
        }
    }

    fun canShowFromBackground(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(appContext)

    private fun show() {
        // Never cover an ongoing call (the screen turns off against the ear during calls).
        val audio = appContext.getSystemService(AudioManager::class.java)
        val mode = audio?.mode ?: AudioManager.MODE_NORMAL
        if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_RINGTONE
        ) {
            return
        }
        runCatching {
            appContext.startActivity(
                Intent(appContext, LockScreenActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                ),
            )
        }
    }

    @Synchronized
    private fun register() {
        if (registered) return
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
        registered = true
    }

    @Synchronized
    private fun unregister() {
        if (!registered) return
        runCatching { appContext.unregisterReceiver(receiver) }
        registered = false
    }
}
