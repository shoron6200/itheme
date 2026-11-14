package com.shoron.mono.launcher.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.launch

/**
 * A–Z fast scroller. Idle: a straight vertical column of letters. While touched, the letters
 * bend toward the finger in a fluid curve (with a light alternating zigzag), the letter under
 * the finger is shown in a bubble, and the list follows. On release everything springs back
 * into a straight line.
 *
 * Motion is applied in graphicsLayer blocks that read the finger position and an Animatable,
 * so dragging redraws layers without recomposing the letters.
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val intensity = remember { Animatable(0f) }
    val fingerY = remember { mutableFloatStateOf(0f) }
    var touched by remember { mutableStateOf<String?>(null) }
    var bubbleLetter by remember { mutableStateOf("") }

    BoxWithConstraints(modifier) {
        if (letters.isEmpty()) return@BoxWithConstraints
        val heightPx = constraints.maxHeight.toFloat()
        val slot: Dp = maxHeight / letters.size
        val slotPx = heightPx / letters.size
        val maxShiftPx = with(density) { 64.dp.toPx() }
        val zigzagPx = with(density) { 5.dp.toPx() }
        val spreadPx = heightPx * 0.42f
        val fontSize = (slot.value * 0.52f).coerceIn(8f, 11f)
        val bubble = 52.dp
        val bubblePx = with(density) { bubble.toPx() }

        // Bubble showing the letter under the finger, riding to the left of the bend.
        Box(
            Modifier
                .size(bubble)
                .graphicsLayer {
                    alpha = intensity.value.coerceIn(0f, 1f)
                    translationX = -maxShiftPx - bubblePx * 0.9f
                    translationY = (fingerY.floatValue - bubblePx / 2f).coerceIn(-bubblePx / 2f, heightPx - bubblePx / 2f)
                    val s = 0.6f + 0.4f * intensity.value
                    scaleX = s
                    scaleY = s
                }
                .background(MonoColors.Foreground.copy(alpha = 0.88f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = bubbleLetter,
                style = TextStyle(color = MonoColors.Ink, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .pointerInput(letters, heightPx) {
                    fun select(index: Int, smooth: Boolean) {
                        val letter = letters[index]
                        if (letter != touched) {
                            touched = letter
                            bubbleLetter = letter
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnLetter(letter, smooth)
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        fingerY.floatValue = down.position.y
                        scope.launch { intensity.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 500f)) }
                        val start = RailMath.indexAt(down.position.y, heightPx, letters.size)
                        var dragged = false
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            fingerY.floatValue = change.position.y.coerceIn(0f, heightPx)
                            val index = RailMath.indexAt(change.position.y, heightPx, letters.size)
                            if (index != start || dragged) {
                                dragged = true
                                select(index, smooth = false)
                            }
                        }
                        // A tap without scrubbing glides smoothly to its section.
                        if (!dragged) select(start, smooth = true)
                        touched = null
                        scope.launch { intensity.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 320f)) }
                    }
                },
        ) {
            letters.forEachIndexed { index, letter ->
                val highlighted = letter == touched || (touched == null && letter == current)
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
                        modifier = Modifier.graphicsLayer {
                            val center = (index + 0.5f) * slotPx
                            val k = intensity.value
                            val b = RailMath.bend(center, fingerY.floatValue, spreadPx) * k
                            translationX = -maxShiftPx * b + RailMath.zigzagSign(index) * zigzagPx * b
                            val s = 1f + 0.45f * b
                            scaleX = s
                            scaleY = s
                        },
                        style = TextStyle(
                            color = if (highlighted) MonoColors.Foreground else MonoColors.Muted,
                            fontFamily = Outfit,
                            fontSize = fontSize.sp,
                            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
