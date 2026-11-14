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
    )

    @Test fun initialLibraryIsComplete() {
        val names = IconManifest.icons.map { it.name }
        required.forEach { assertTrue("missing icon: $it", it in names) }
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
