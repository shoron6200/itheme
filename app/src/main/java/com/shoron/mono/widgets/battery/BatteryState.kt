package com.shoron.mono.widgets.battery

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

enum class PowerSource { None, AC, USB, Wireless, Dock }

@Immutable
data class BatteryState(
    val percent: Int,
    val charging: Boolean,
    val full: Boolean,
    val source: PowerSource,
) {
    companion object {
        // Mirrors android.os.BatteryManager constants (kept here so parsing is JVM-testable).
        const val STATUS_CHARGING = 2
        const val STATUS_FULL = 5
        const val PLUGGED_AC = 1
        const val PLUGGED_USB = 2
        const val PLUGGED_WIRELESS = 4
        const val PLUGGED_DOCK = 8

        /** Builds state from the raw ACTION_BATTERY_CHANGED extras. */
        fun from(level: Int, scale: Int, status: Int, plugged: Int): BatteryState {
            val percent = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt().coerceIn(0, 100) else 0
            val source = when {
                plugged and PLUGGED_AC != 0 -> PowerSource.AC
                plugged and PLUGGED_USB != 0 -> PowerSource.USB
                plugged and PLUGGED_WIRELESS != 0 -> PowerSource.Wireless
                plugged and PLUGGED_DOCK != 0 -> PowerSource.Dock
                else -> PowerSource.None
            }
            val full = status == STATUS_FULL || (source != PowerSource.None && percent >= 100)
            val charging = status == STATUS_CHARGING || (source != PowerSource.None && !full)
            return BatteryState(percent, charging, full, source)
        }
    }
}
