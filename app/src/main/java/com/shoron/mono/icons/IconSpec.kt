package com.shoron.mono.icons

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/**
 * One MONO Thin Line icon and the apps it represents (from design/icons/manifest.json, compiled
 * into [com.shoron.mono.icons.generated.IconManifest] by tools/icons/build_icons.py).
 */
@Immutable
data class IconSpec(
    val name: String,
    @DrawableRes val drawable: Int,
    val packages: Set<String>,
    val components: Set<String>,
    val role: String?,
    val labels: List<String>,
    /** Production batch the icon was drawn in (design/icons/BATCHES.md); 0 = unknown. */
    val batch: Int = 0,
)

/** System roles an icon can follow, so OEM apps (e.g. Realme/ColorOS) get the right icon. */
object IconRoles {
    const val DIALER = "dialer"
    const val SMS = "sms"
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
    const val CLOCK = "clock"
    const val CALENDAR = "calendar"
}

/** Pure matching rules (JVM-tested). */
object IconMatcher {

    /**
     * Picks the icon for one launchable activity.
     * 1. Exact component (`package/Activity`) wins.
     * 2. Package (listed, or currently filling the icon's role). If the package has several
     *    launcher activities and the icon declares label hints, only activities whose label
     *    contains a hint match — e.g. "Phone" but not "Contacts" inside a combined dialer app.
     */
    fun match(
        packageName: String,
        className: String,
        label: String,
        activitiesInPackage: Int,
        specs: List<IconSpec>,
        rolePackages: Map<String, String?>,
    ): IconSpec? {
        val component = "$packageName/$className"
        specs.firstOrNull { component in it.components }?.let { return it }
        val lowerLabel = label.lowercase()
        return specs.firstOrNull { spec ->
            val byPackage = packageName in spec.packages ||
                (spec.role != null && rolePackages[spec.role] == packageName)
            byPackage && (
                activitiesInPackage <= 1 ||
                    spec.labels.isEmpty() ||
                    spec.labels.any { lowerLabel.contains(it) }
                )
        }
    }
}
