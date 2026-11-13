package com.shoron.mono.widgets.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.time.LocalDateTime

/**
 * Emits the current time once per minute using the system TIME_TICK broadcast
 * (plus time/zone changes). No timers, no polling; the receiver is registered only
 * while someone collects, so it stops when the home screen is not visible.
 */
class TimeTicker(context: Context) {

    private val appContext = context.applicationContext

    val now: Flow<LocalDateTime> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(LocalDateTime.now())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        // These are protected system broadcasts, which are delivered to not-exported receivers.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
        trySend(LocalDateTime.now())
        awaitClose { appContext.unregisterReceiver(receiver) }
    }.conflate()
}
