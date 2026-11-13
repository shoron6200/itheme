package com.shoron.mono.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultPinsTest {

    private val phone = testApp("Phone", "com.dialer")
    private val chrome = testApp("Chrome", "com.android.chrome")
    private val alpha = testApp("Alpha")
    private val beta = testApp("Beta")
    private val workMail = testApp("Mail", "com.mail", work = true)

    @Test fun preferredAppsComeFirstThenAlphabeticalFill() {
        val pins = DefaultPins.choose(
            apps = listOf(alpha, beta, chrome, phone),
            preferredPackages = listOf("com.dialer", "missing.app", "com.android.chrome"),
            count = 3,
        )
        assertEquals(listOf(phone.key, chrome.key, alpha.key), pins)
    }

    @Test fun workProfileAppsAreNeverDefaultPins() {
        val pins = DefaultPins.choose(listOf(workMail, alpha), listOf("com.mail"))
        assertEquals(listOf(alpha.key), pins)
    }

    @Test fun pinnedAppsResolveInOrderAndSkipMissing() {
        val resolved = PinnedApps.resolve(listOf(alpha, beta, chrome), listOf(chrome.key, "gone|0", alpha.key))
        assertEquals(listOf(chrome, alpha), resolved)
    }
}
