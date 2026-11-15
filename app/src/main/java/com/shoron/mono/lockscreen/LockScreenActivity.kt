package com.shoron.mono.lockscreen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.shoron.mono.R
import com.shoron.mono.data.LockSettings
import com.shoron.mono.power.Diagnostics
import com.shoron.mono.power.ScreenControl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
class LockScreenActivity : ComponentActivity(), SensorEventListener {

    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }
    private val power by lazy { getSystemService(PowerManager::class.java) }
    private val sensors by lazy { getSystemService(SensorManager::class.java) }
    private var preview = false
    private var leaving = false

    /** Latest lock-screen settings (position, contrast, power behaviour). */
    private var settings = LockSettings()

    /** Elapsed-realtime of the last touch, for the idle screen-off timeout. */
    private val lastInteraction = MutableStateFlow(SystemClock.elapsedRealtime())

    /** True while the proximity sensor is covered (pocket / face-down): touches are blocked. */
    private val pocketCovered = mutableStateOf(false)
    private var pocketSleepJob: Job? = null

    private val unlockedReceiver = object : BroadcastReceiver() {
        // Fingerprint/face unlock while MONO's screen was on top.
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!preview) leave()
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
        // USER_PRESENT is sent by SystemUI (an app uid, not the system uid), so a NOT_EXPORTED
        // receiver never gets it on Android 13+ — the root cause of the extra swipe after a
        // fingerprint unlock. It is a protected broadcast (only the platform can send it), so
        // an exported receiver cannot be spoofed by other apps.
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockedReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockedReceiver, filter)
        }
        // Keep a copy of the lock settings (power behaviour, pocket, idle timeout).
        lifecycleScope.launch {
            monoContainer.widgetPreferences.lock.collect { settings = it }
        }
        // Safety net for OEM builds that delay or drop USER_PRESENT: for a few seconds after the
        // screen comes on (when a fingerprint unlock happens), check whether the keyguard is gone.
        // Bounded and screen-on only — previously this polled 4x/s for as long as it was resumed.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val until = SystemClock.elapsedRealtime() + UNLOCK_WATCH_MS
                while (SystemClock.elapsedRealtime() < until && power.isInteractive) {
                    Diagnostics.count(Diagnostics.LOCK_UNLOCK_CHECKS)
                    if (!preview && !keyguard.isKeyguardLocked) {
                        leave()
                        break
                    }
                    delay(UNLOCK_CHECK_MS)
                }
            }
        }
        // Idle timeout: while MONO covers the keyguard, Android applies the normal screen timeout
        // (often 30 s-2 min) instead of the short lock-screen one — the screen stayed on after
        // lift-to-wake and drained battery. Turn it off after the configured idle time instead.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                lastInteraction.collectLatest {
                    delay(settings.idleTimeoutSeconds * 1000L)
                    if (power.isInteractive) sleepOrHandOff()
                }
            }
        }
        // Back never skips the lock; in preview it simply closes.
        onBackPressedDispatcher.addCallback(this) { if (preview) finish() }

        val container = monoContainer
        setContent {
            MonoTheme {
                CompositionLocalProvider(LocalWallpaperLoader provides container.wallpaperRepository) {
                    LockScreenRoute(
                        onUnlock = ::unlock,
                        onDoubleTap = ::onDoubleTap,
                        pocketCovered = pocketCovered,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        leaving = false
    }

    override fun onStart() {
        super.onStart()
        isVisible = true
    }

    override fun onStop() {
        isVisible = false
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        Diagnostics.count(Diagnostics.LOCK_SHOWN)
        lastInteraction.value = SystemClock.elapsedRealtime()
        // Already unlocked (e.g. fingerprint on the power button woke and unlocked the phone):
        // get out of the way so the previous app (or MONO home) is shown — no extra swipe.
        if (!preview && power.isInteractive && !keyguard.isKeyguardLocked) {
            leave()
            return
        }
        // Proximity listener only while this screen is visible; released in onPause.
        if (settings.pocketProtection) {
            sensors?.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
                sensors?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        sensors?.unregisterListener(this)
        pocketSleepJob?.cancel()
        pocketCovered.value = false
        super.onPause()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!pocketCovered.value) lastInteraction.value = SystemClock.elapsedRealtime()
    }

    // ------------------------------------------------------------------ pocket protection

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val covered = event.values.firstOrNull()?.let { it < event.sensor.maximumRange.coerceAtMost(5f) } ?: false
        if (covered == pocketCovered.value) return
        pocketCovered.value = covered
        pocketSleepJob?.cancel()
        if (covered) {
            Diagnostics.count(Diagnostics.POCKET_EVENTS)
            // Still covered after a moment: the phone woke in a pocket — turn the screen off.
            pocketSleepJob = lifecycleScope.launch {
                delay(POCKET_SLEEP_MS)
                if (pocketCovered.value && power.isInteractive) sleepOrHandOff()
            }
        } else {
            lastInteraction.value = SystemClock.elapsedRealtime()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // ------------------------------------------------------------------ power actions

    private fun onDoubleTap() {
        if (preview || !settings.doubleTapToSleep) return
        if (!ScreenControl.sleep()) {
            Toast.makeText(this, R.string.lock_double_tap_needs_access, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Turn the display off. With MONO's accessibility service this is like the power button
     * (MONO's lock screen stays for the next wake). Without it, step aside so Android's own
     * lock screen — with its short timeout and the OEM pocket handling — takes over.
     */
    private fun sleepOrHandOff() {
        if (preview) {
            finish()
            return
        }
        if (!keyguard.isKeyguardLocked) {
            leave()
            return
        }
        if (!ScreenControl.sleep()) finish()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockedReceiver) }
        super.onDestroy()
    }

    private fun unlock() {
        if (preview) {
            finish()
            return
        }
        if (!keyguard.isKeyguardLocked) {
            leave()
            return
        }
        // Android shows its own PIN / pattern / password / biometric prompt.
        keyguard.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    leave()
                }

                override fun onDismissCancelled() {
                    // User backed out of the bouncer: stay on the MONO lock screen.
                }

                override fun onDismissError() {
                    // OEM refused the request: reveal Android's own lock screen (still secure).
                    finish()
                }
            },
        )
    }

    /**
     * After an unlock, simply close this screen (no animation). Its task sits on top of the
     * task that was in front when the screen turned off, so Android returns to exactly that:
     * ChatGPT stays ChatGPT, Chrome stays Chrome, MONO home stays MONO home. (The previous
     * version started MONO's HOME intent here, which is why apps were replaced by MONO.)
     */
    private fun leave() {
        if (leaving || isFinishing) return
        leaving = true
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val EXTRA_PREVIEW = "preview"
        private const val UNLOCK_CHECK_MS = 300L
        private const val UNLOCK_WATCH_MS = 12_000L
        private const val POCKET_SLEEP_MS = 1_500L

        /** True while the MONO lock screen is on screen; prevents duplicate launches. */
        @Volatile
        var isVisible: Boolean = false
            private set

        fun previewIntent(context: Context): Intent =
            Intent(context, LockScreenActivity::class.java)
                .putExtra(EXTRA_PREVIEW, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun LockScreenRoute(
    onUnlock: () -> Unit,
    onDoubleTap: () -> Unit,
    pocketCovered: State<Boolean>,
) {
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
        onDoubleTap = onDoubleTap,
        pocketCovered = pocketCovered.value,
    )
}
