package com.shoron.mono.settings

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.shoron.mono.apps.LocalIconLoader
import com.shoron.mono.wallpaper.LocalWallpaperLoader
import com.shoron.mono.core.theme.MonoTheme
import com.shoron.mono.monoContainer

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = monoContainer
        setContent {
            MonoTheme {
                CompositionLocalProvider(
                    LocalIconLoader provides container.appRepository,
                    LocalWallpaperLoader provides container.wallpaperRepository,
                ) {
                    SettingsNavHost(onExit = ::finish)
                }
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
