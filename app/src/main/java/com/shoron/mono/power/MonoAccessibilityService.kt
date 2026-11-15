package com.shoron.mono.power

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Exists only so MONO can turn the display off like the power button
 * (GLOBAL_ACTION_LOCK_SCREEN, Android 9+): double-tap to sleep, idle timeout and pocket
 * protection on the MONO lock screen.
 *
 * It subscribes to no accessibility events, cannot read window content, and holds no wake
 * locks. The system binds it only while the user has enabled it in Accessibility settings.
 * Device-admin lockNow() was rejected: it forces PIN entry next time (disables fingerprint).
 */
class MonoAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        internal var instance: MonoAccessibilityService? = null
            private set
    }
}
