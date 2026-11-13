package com.shoron.mono.apps

fun testApp(label: String, pkg: String = "pkg.${label.lowercase().replace(' ', '.')}", work: Boolean = false) =
    AppEntry(
        key = AppKeys.of(pkg, "$pkg.Main", if (work) 10L else 0L),
        label = label,
        packageName = pkg,
        className = "$pkg.Main",
        userSerial = if (work) 10L else 0L,
        isWorkProfile = work,
    )
