package com.shoron.mono.launcher.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit

/**
 * Zigzag A–Z fast scroller. Letters sit in equal-height slots and alternate left/right so the
 * rail reads as a wave. Tap a letter to glide to its section; drag along the rail to scrub
 * (instant jumps). The letter of the section at the top of the list is highlighted.
 */
@Composable
fun AlphabetRail(
    letters: List<String>,
    current: String?,
    onLetter: (letter: String, smooth: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnLetter by rememberUpdatedState(onLetter)
    val haptics = LocalHapticFeedback.current
    var touched by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier) {
        if (letters.isEmpty()) return@BoxWithConstraints
        val slot: Dp = maxHeight / letters.size
        val amplitude: Dp = (maxWidth * 0.22f).coerceAtMost(10.dp)
        val baseSize = (slot.value * 0.52f).coerceIn(8f, 11f)
        val heightPx = constraints.maxHeight.toFloat()

        Column(
            Modifier
                .fillMaxWidth()
                .pointerInput(letters, heightPx) {
                    fun indexAt(y: Float) = (y / heightPx * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    fun pick(index: Int, smooth: Boolean) {
                        val letter = letters[index]
                        if (letter != touched) {
                            touched = letter
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnLetter(letter, smooth)
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val start = indexAt(down.position.y)
                        var dragged = false
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            val index = indexAt(change.position.y)
                            if (index != start || dragged) {
                                dragged = true
                                pick(index, smooth = false)
                            }
                        }
                        // A tap (no scrubbing) glides smoothly to the section.
                        if (!dragged) pick(start, smooth = true)
                        touched = null
                    }
                },
        ) {
            letters.forEachIndexed { index, letter ->
                val isTouched = letter == touched
                val isCurrent = letter == current
                val scale by animateFloatAsState(
                    targetValue = if (isTouched) 1.6f else if (isCurrent) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
                    label = "railLetter",
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(slot)
                        .semantics {
                            role = Role.Button
                            onClick(label = letter) {
                                currentOnLetter(letter, true)
                                true
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter,
                        modifier = Modifier
                            .offset(x = if (index % 2 == 0) -amplitude else amplitude)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        style = TextStyle(
                            color = when {
                                isTouched || isCurrent -> MonoColors.Foreground
                                else -> MonoColors.Faint
                            },
                            fontFamily = Outfit,
                            fontSize = baseSize.sp,
                            fontWeight = if (isTouched || isCurrent) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
