package com.shoron.mono.apps

import androidx.compose.runtime.Immutable

/**
 * A launchable activity for one user profile. Pure Kotlin (no Android types) so it is
 * cheap to compare, stable for Compose, and unit-testable on the JVM.
 */
@Immutable
data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val className: String,
    val userSerial: Long,
    val isWorkProfile: Boolean,
)

object AppKeys {
    /** Stable identity: component + profile serial, e.g. `com.android.chrome/com.google.Main|0`. */
    fun of(packageName: String, className: String, userSerial: Long): String =
        "$packageName/$className|$userSerial"
}
