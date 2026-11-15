package com.shoron.mono.icons

import com.shoron.mono.icons.generated.IconManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconMatcherTest {

    private val specs = IconManifest.icons
    private val noRoles = emptyMap<String, String?>()

    private fun match(pkg: String, cls: String = "$pkg.Main", label: String = "App", count: Int = 1, roles: Map<String, String?> = noRoles) =
        IconMatcher.match(pkg, cls, label, count, specs, roles)?.name

    @Test fun mapsBrandPackages() {
        assertEquals("whatsapp", match("com.whatsapp"))
        assertEquals("whatsapp", match("com.whatsapp.w4b"))
        assertEquals("chrome", match("com.android.chrome"))
        assertEquals("youtube", match("com.google.android.youtube"))
        assertEquals("playstore", match("com.android.vending"))
        assertEquals("drive", match("com.google.android.apps.docs", label = "Drive"))
        assertEquals("chatgpt", match("com.openai.chatgpt"))
        assertEquals("x", match("com.twitter.android"))
        assertEquals("tiktok", match("com.zhiliaoapp.musically"))
    }

    @Test fun unknownAppsFallBackToOriginalIcon() {
        assertNull(match("com.example.unknown"))
    }

    @Test fun roleMapsOemSystemApps() {
        // e.g. Realme/ColorOS: the default dialer is a vendor package not listed in the manifest.
        val roles = mapOf(IconRoles.DIALER to "com.vendor.dialer", IconRoles.CLOCK to "com.vendor.clock")
        assertEquals("phone", match("com.vendor.dialer", roles = roles))
        assertEquals("clock", match("com.vendor.clock", roles = roles))
        assertNull(match("com.vendor.notes", roles = roles))
    }

    @Test fun multiActivityPackagesUseLabelHints() {
        // A combined contacts app with "Phone" and "Contacts" launcher entries.
        val roles = mapOf(IconRoles.DIALER to "com.android.contacts")
        assertEquals("phone", match("com.android.contacts", label = "Phone", count = 2, roles = roles))
        assertEquals("phone", match("com.android.contacts", label = "ODialer", count = 2, roles = roles))
        // ...while its "Contacts" entry gets the Contacts icon (batch 2), not the Phone icon.
        assertEquals("contacts", match("com.android.contacts", label = "Contacts", count = 2, roles = roles))
    }

    @Test fun exactComponentWinsOverPackage() {
        val custom = listOf(
            IconSpec("a", 1, setOf("com.x"), emptySet(), null, emptyList()),
            IconSpec("b", 2, emptySet(), setOf("com.x/com.x.Second"), null, emptyList()),
        )
        assertEquals("b", IconMatcher.match("com.x", "com.x.Second", "Second", 2, custom, noRoles)?.name)
        assertEquals("a", IconMatcher.match("com.x", "com.x.First", "First", 2, custom, noRoles)?.name)
    }
}
