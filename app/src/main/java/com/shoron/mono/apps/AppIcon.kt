package com.shoron.mono.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shoron.mono.core.design.MonoIconFilter
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.icons.LocalThemedIcons
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource

private val ThinLineTint = ColorFilter.tint(MonoColors.Foreground)

/** Lazily loaded, cached, monochrome app icon. */
@Composable
fun AppIcon(entry: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val themed = LocalThemedIcons.current[entry.key]
    if (themed != null) {
        // MONO Thin Line vector (48-unit canvas), tinted to the foreground colour.
        Image(
            painter = painterResource(themed),
            contentDescription = null,
            modifier = modifier.size(size),
            colorFilter = ThinLineTint,
        )
        return
    }
    val loader = LocalIconLoader.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(loader.peek(entry, sizePx), entry.key, sizePx) {
        value = loader.peek(entry, sizePx) ?: loader.load(entry, sizePx)
    }
    val sized = modifier.size(size)
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = null,
            modifier = sized,
            colorFilter = MonoIconFilter,
            filterQuality = FilterQuality.Medium,
        )
    } else {
        Box(sized.border(1.dp, MonoColors.Hairline, CircleShape))
    }
}
