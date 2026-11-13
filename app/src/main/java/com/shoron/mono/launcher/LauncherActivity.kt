package com.shoron.mono.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.shoron.mono.apps.LocalIconLoader
import com.shoron.mono.core.theme.MonoTheme
import com.shoron.mono.monoContainer
import com.shoron.mono.settings.SettingsActivity
import kotlinx.coroutines.flow.MutableSharedFlow

/** The HOME activity. Single instance, never in recents, no restart on config changes. */
class LauncherActivity : ComponentActivity() {

    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = monoContainer
        setContent {
            MonoTheme {
                CompositionLocalProvider(LocalIconLoader provides container.appRepository) {
                    LauncherRoot(
                        homePresses = homePresses,
                        onOpenSettings = ::openSettings,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePresses.tryEmit(Unit)
        }
    }

    private fun openSettings() {
        startActivity(SettingsActivity.intent(this))
    }
}
