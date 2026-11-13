package com.shoron.mono.widgets.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Battery level and charging state from the system's ACTION_BATTERY_CHANGED broadcast.
 * It fires only on change (no polling), needs no permission, and the receiver is registered
 * only while a visible screen collects this flow.
 */
class BatteryMonitor(context: Context) {

    private val appContext = context.applicationContext

    val state: Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { trySend(parse(it)) }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // Sticky broadcast: registering returns the current state immediately.
        val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
        sticky?.let { trySend(parse(it)) }
        awaitClose { appContext.unregisterReceiver(receiver) }
    }.conflate().distinctUntilChanged()

    private fun parse(intent: Intent) = BatteryState.from(
        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
        scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100),
        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
        plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
    )
}
