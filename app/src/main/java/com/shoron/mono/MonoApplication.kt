package com.shoron.mono

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context

class MonoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Keep icons warm while the launcher is merely hidden; drop them only under real pressure.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            container.iconCache.clear()
        }
    }
}

val Context.monoContainer: AppContainer
    get() = (applicationContext as MonoApplication).container
