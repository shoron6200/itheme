package com.shoron.mono.icons

import com.shoron.mono.icons.generated.IconManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconManifestTest {

    private val required = listOf(
        "whatsapp", "telegram", "chrome", "youtube", "spotify", "instagram", "facebook", "messenger",
        "gmail", "maps", "photos", "camera", "phone", "messages", "settings", "playstore", "drive",
        "files", "calendar", "clock",
        // batch 2
        "contacts", "calculator", "gallery", "music", "notes", "weather", "recorder", "google",
        "gemini", "chatgpt", "x", "tiktok", "snapchat", "linkedin", "netflix", "discord", "keep",
        "meet", "translate",
    )

    /** Exact package mappings from the 2026-11-15 missing-icon request (batches 3–8). */
    private val requestSamples = mapOf(
        "com.cloudflare.onedotonedotonedotone" to "cloudflare",
        "com.anthropic.claude" to "claude",
        "com.deepseek.chat" to "deepseek",
        "ai.x.grok" to "grok",
        "com.bKash.customerapp" to "bkash",
        "com.konasl.nagad" to "nagad",
        "com.binance.dev" to "binance",
        "org.mozilla.firefox" to "firefox",
        "com.brave.browser" to "brave",
        "com.duckduckgo.mobile.android" to "duckduckgo",
        "com.instagram.barcelona" to "threads",
        "com.reddit.frontpage" to "reddit",
        "com.waze" to "waze",
        "org.videolan.vlc" to "vlc",
        "net.mullvad.mullvadvpn" to "vpn",
        "com.exteragram.messenger" to "telegram",
        "com.mixplorer" to "files",
        "com.socialnmobile.dictapps.notepad.color.note" to "notes",
        "cn.wps.moffice_eng" to "office",
        "com.oneplus.brickmode" to "zen",
    )

    @Test fun initialLibraryIsComplete() {
        val names = IconManifest.icons.map { it.name }
        required.forEach { assertTrue("missing icon: $it", it in names) }
    }

    @Test fun requestedPackagesMapToTheirIcons() {
        val owner = IconManifest.icons.flatMap { spec -> spec.packages.map { it to spec.name } }.toMap()
        requestSamples.forEach { (pkg, icon) -> assertEquals(pkg, icon, owner[pkg]) }
        // Unknown app from the request keeps its original icon instead of a guessed one.
        assertEquals(null, owner["com.sndemo.app"])
    }

    @Test fun everyIconHasABatch() {
        IconManifest.icons.forEach { assertTrue("${it.name} has no batch", it.batch in 1..8) }
        assertEquals(146, IconManifest.icons.size)
    }

    @Test fun namesAndPackagesAreUnique() {
        val names = IconManifest.icons.map { it.name }
        assertEquals(names.size, names.toSet().size)
        val packages = IconManifest.icons.flatMap { it.packages }
        assertEquals("a package is mapped to two icons", packages.size, packages.toSet().size)
    }

    @Test fun everyIconHasADrawableAndATarget() {
        IconManifest.icons.forEach { spec ->
            assertTrue("${spec.name} has no drawable", spec.drawable != 0)
            assertTrue("${spec.name} maps to nothing", spec.packages.isNotEmpty() || spec.components.isNotEmpty() || spec.role != null)
        }
    }
}
