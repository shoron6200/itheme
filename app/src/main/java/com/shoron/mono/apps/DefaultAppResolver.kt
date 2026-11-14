package com.shoron.mono.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import com.shoron.mono.icons.IconRoles

/** Finds the user's default phone, messages, camera, browser, store and settings apps. */
class DefaultAppResolver(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun preferredPackages(): List<String> = listOfNotNull(
        resolve(Intent(Intent.ACTION_DIAL)),
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull(),
        resolve(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
        resolve(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))),
        PLAY_STORE,
        resolve(Intent(Settings.ACTION_SETTINGS)),
    ).distinct()

    /** Package currently filling each icon role (null when unset or ambiguous). */
    fun rolePackages(): Map<String, String?> = mapOf(
        IconRoles.DIALER to resolve(Intent(Intent.ACTION_DIAL)),
        IconRoles.SMS to runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull(),
        IconRoles.CAMERA to (
            resolve(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                ?: resolve(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            ),
        IconRoles.SETTINGS to resolve(Intent(Settings.ACTION_SETTINGS)),
        IconRoles.CLOCK to resolve(Intent(AlarmClock.ACTION_SHOW_ALARMS)),
        IconRoles.CALENDAR to resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)),
    )

    private fun resolve(intent: Intent): String? =
        runCatching { pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) }
            .getOrNull()
            ?.activityInfo
            ?.packageName
            ?.takeUnless { it == "android" } // the system chooser, i.e. no default set

    private companion object {
        const val PLAY_STORE = "com.android.vending"
    }
}
