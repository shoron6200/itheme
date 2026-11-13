package com.shoron.mono.lockscreen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoron.mono.core.theme.MonoTheme
import com.shoron.mono.monoContainer
import com.shoron.mono.wallpaper.LocalWallpaperLoader

/**
 * Shown *over* Android's keyguard (showWhenLocked). It never dismisses or disables the
 * keyguard itself: swiping up calls KeyguardManager.requestDismissKeyguard, which shows the
 * system's own PIN / pattern / password / biometric prompt. If the phone is unlocked any other
 * way (e.g. fingerprint), it closes itself.
 */
class LockScreenActivity : ComponentActivity() {

    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }
    private val power by lazy { getSystemService(PowerManager::class.java) }
    private var preview = false

    private val unlockedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!preview) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Fully immersive: draw under the camera cutout and hide both status and navigation bars
        // (a swipe from the edge reveals them temporarily).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(unlockedReceiver, filter)
        }
        // Back never skips the lock; in preview it simply closes.
        onBackPressedDispatcher.addCallback(this) { if (preview) finish() }

        val container = monoContainer
        setContent {
            MonoTheme {
                CompositionLocalProvider(LocalWallpaperLoader provides container.wallpaperRepository) {
                    LockScreenRoute(onUnlock = ::unlock)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
    }

    override fun onResume() {
        super.onResume()
        // Woken but not locked (e.g. within the lock delay): nothing to cover.
        if (!preview && power.isInteractive && !keyguard.isKeyguardLocked) finish()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockedReceiver) }
        super.onDestroy()
    }

    private fun unlock() {
        if (preview || !keyguard.isKeyguardLocked) {
            finish()
            return
        }
        keyguard.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    finish()
                }
            },
        )
    }

    companion object {
        private const val EXTRA_PREVIEW = "preview"

        fun previewIntent(context: Context): Intent =
            Intent(context, LockScreenActivity::class.java)
                .putExtra(EXTRA_PREVIEW, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun LockScreenRoute(onUnlock: () -> Unit) {
    val viewModel: LockViewModel = viewModel(factory = LockViewModel.Factory)
    val wallpaper by viewModel.wallpaper.collectAsStateWithLifecycle()
    val lock by viewModel.lock.collectAsStateWithLifecycle()
    val settings by viewModel.widgetSettings.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val time = viewModel.time.collectAsStateWithLifecycle()
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val mediaActions = remember(viewModel) {
        MediaActions(
            playPause = viewModel::playPause,
            previous = viewModel::previous,
            next = viewModel::next,
            seekTo = viewModel::seekTo,
            dismiss = viewModel::dismissMedia,
        )
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onVisible()
        onPauseOrDispose { }
    }

    LockScreenContent(
        wallpaper = wallpaper,
        lock = lock,
        use24Hour = settings.use24Hour,
        showSeconds = settings.showSeconds,
        clock = clock,
        time = time,
        weather = weather,
        battery = battery,
        media = media,
        mediaActions = mediaActions,
        onUnlock = onUnlock,
    )
}
