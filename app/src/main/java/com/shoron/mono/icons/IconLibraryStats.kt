package com.shoron.mono.icons

/** One production batch of the Thin Line library and how much of it this phone uses. */
data class BatchStat(val batch: Int, val icons: Int, val inUse: Int)

/** Coverage numbers for Settings → Icons (pure, JVM-tested). */
data class IconCoverage(
    /** Launchable apps on this phone. */
    val totalApps: Int,
    /** Apps showing a custom Thin Line icon. */
    val customApps: Int,
    /** Icons in the bundled library. */
    val libraryIcons: Int,
    val batches: List<BatchStat>,
) {
    val missingApps: Int get() = (totalApps - customApps).coerceAtLeast(0)

    /** 0..1 share of apps with a custom icon (0 when no apps are known yet). */
    val fraction: Float get() = if (totalApps <= 0) 0f else customApps.coerceAtMost(totalApps).toFloat() / totalApps

    val percent: Int get() = (fraction * 100).toInt()
}

object IconLibraryStats {

    /**
     * @param specs the bundled library.
     * @param totalApps launchable apps on this phone.
     * @param themed app key → drawable of the Thin Line icon it shows.
     */
    fun coverage(specs: List<IconSpec>, totalApps: Int, themed: Map<String, Int>): IconCoverage {
        val usedDrawables = themed.values.toSet()
        val batches = specs.groupBy { it.batch }
            .toSortedMap()
            .map { (batch, icons) -> BatchStat(batch, icons.size, icons.count { it.drawable in usedDrawables }) }
        return IconCoverage(
            totalApps = totalApps,
            customApps = themed.size,
            libraryIcons = specs.size,
            batches = batches,
        )
    }
}
