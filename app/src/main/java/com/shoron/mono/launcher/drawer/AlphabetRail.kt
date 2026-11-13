package com.shoron.mono.launcher.drawer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shoron.mono.core.theme.MonoColors

/** Vertical A–Z fast scroller. Touch or drag to jump to a section. */
@Composable
fun AlphabetRail(
    letters: List<String>,
    onLetter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnLetter by rememberUpdatedState(onLetter)
    val haptics = LocalHapticFeedback.current
    var active by remember { mutableStateOf<String?>(null) }
    var heightPx by remember { mutableIntStateOf(0) }

    Column(
        modifier
            .onSizeChanged { heightPx = it.height }
            .pointerInput(letters) {
                fun pick(y: Float) {
                    if (letters.isEmpty() || heightPx == 0) return
                    val i = (y / heightPx * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    val letter = letters[i]
                    if (letter != active) {
                        active = letter
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnLetter(letter)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pick(down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        pick(change.position.y)
                    }
                    active = null
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val isActive = letter == active
            Text(
                text = letter,
                color = if (isActive) MonoColors.Foreground else MonoColors.Faint,
                fontSize = if (isActive) 14.sp else 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
