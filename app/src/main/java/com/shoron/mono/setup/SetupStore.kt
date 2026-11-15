package com.shoron.mono.setup

import android.content.Context

/** Remembers onboarding completion, skipped steps and steps the user went to Settings for. */
class SetupStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("setup", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_DONE, value).apply()

    fun isSkipped(id: SetupStepId) = id.name in (prefs.getStringSet(KEY_SKIPPED, emptySet()) ?: emptySet())

    fun setSkipped(id: SetupStepId, skipped: Boolean) {
        val set = (prefs.getStringSet(KEY_SKIPPED, emptySet()) ?: emptySet()).toMutableSet()
        if (skipped) set += id.name else set -= id.name
        prefs.edit().putStringSet(KEY_SKIPPED, set).apply()
    }

    /** "Waiting for user": opened in Settings this install, not yet granted. */
    fun wasOpened(id: SetupStepId) = id.name in (prefs.getStringSet(KEY_OPENED, emptySet()) ?: emptySet())

    fun markOpened(id: SetupStepId) {
        val set = (prefs.getStringSet(KEY_OPENED, emptySet()) ?: emptySet()).toMutableSet()
        set += id.name
        prefs.edit().putStringSet(KEY_OPENED, set).apply()
    }

    private companion object {
        const val KEY_DONE = "onboarding_done"
        const val KEY_SKIPPED = "skipped"
        const val KEY_OPENED = "opened"
    }
}
