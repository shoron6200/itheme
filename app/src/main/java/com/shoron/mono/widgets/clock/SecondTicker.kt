package com.shoron.mono.widgets.clock

import com.shoron.mono.power.Diagnostics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalTime

/**
 * Emits the wall-clock time at each second boundary. Cold: it only runs while collected,
 * and screens collect it with collectAsStateWithLifecycle, so it stops when not visible.
 * Reading LocalTime.now() on every tick picks up time-zone and manual time changes at once.
 */
object SecondTicker {
    val ticks: Flow<LocalTime> = flow {
        while (true) {
            val now = LocalTime.now()
            emit(now)
            Diagnostics.count(Diagnostics.SECOND_TICKS)
            delay(1_000L - now.nano / 1_000_000L)
        }
    }
}
