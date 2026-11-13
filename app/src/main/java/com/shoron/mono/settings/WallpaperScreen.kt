package com.shoron.mono.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoron.mono.R
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import com.shoron.mono.core.theme.Sacramento
import com.shoron.mono.wallpaper.WallpaperCatalog
import com.shoron.mono.wallpaper.WallpaperImage
import com.shoron.mono.wallpaper.WallpaperMath
import com.shoron.mono.wallpaper.WallpaperSettings
import com.shoron.mono.wallpaper.WallpaperViewModel
import kotlinx.coroutines.launch

private val PreviewShape = RoundedCornerShape(20.dp)
private val ThumbShape = RoundedCornerShape(10.dp)

@Composable
fun WallpaperScreen(onBack: () -> Unit) {
    val viewModel: WallpaperViewModel = viewModel(factory = WallpaperViewModel.Factory)
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val customVersion by viewModel.customVersion.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // System photo picker: no storage permission required.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                if (!viewModel.importPhoto(uri)) {
                    Toast.makeText(context, R.string.wallpaper_import_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val settings = draft ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = MonoColors.Foreground,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_wallpaper),
                color = MonoColors.Foreground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Same aspect ratio as this device's screen, so the crop matches the home screen.
            val configuration = LocalConfiguration.current
            val aspect = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.coerceAtLeast(1)
            val previewHeight = minOf(maxHeight, maxWidth / aspect)
            WallpaperPreview(
                settings = settings,
                onBias = viewModel::setBias,
                modifier = Modifier.size(width = previewHeight * aspect, height = previewHeight),
            )
        }

        Text(
            text = stringResource(R.string.wallpaper_drag_hint),
            color = MonoColors.Faint,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(WallpaperCatalog.builtIns, key = { it.id }) { builtIn ->
                Thumbnail(
                    label = stringResource(builtIn.nameRes),
                    selected = settings.sourceId == builtIn.id,
                    onClick = { viewModel.selectBuiltIn(builtIn.id) },
                ) {
                    WallpaperImage(WallpaperSettings(sourceId = builtIn.id, dim = 0f), Modifier.fillMaxSize())
                }
            }
            if (customVersion > 0) {
                item(key = "custom") {
                    Thumbnail(
                        label = stringResource(R.string.wallpaper_custom),
                        selected = settings.isCustom,
                        onClick = viewModel::selectCustom,
                    ) {
                        WallpaperImage(
                            WallpaperSettings(
                                sourceId = WallpaperSettings.CUSTOM_SOURCE,
                                customVersion = customVersion,
                                dim = 0f,
                                monochrome = settings.monochrome,
                            ),
                            Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            item(key = "pick") {
                Thumbnail(
                    label = stringResource(R.string.wallpaper_pick_photo),
                    selected = false,
                    onClick = {
                        if (!importing) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                ) {
                    Box(Modifier.fillMaxSize().background(MonoColors.Charcoal), contentAlignment = Alignment.Center) {
                        if (importing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MonoColors.Foreground,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = MonoColors.Foreground)
                        }
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.wallpaper_dim),
                    color = MonoColors.Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.width(104.dp),
                )
                Slider(
                    value = settings.dim,
                    onValueChange = viewModel::setDim,
                    valueRange = 0f..WallpaperSettings.MAX_DIM,
                    colors = SliderDefaults.colors(
                        thumbColor = MonoColors.Foreground,
                        activeTrackColor = MonoColors.Foreground,
                        inactiveTrackColor = MonoColors.Hairline,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.wallpaper_monochrome),
                    color = MonoColors.Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = settings.monochrome,
                    onCheckedChange = viewModel::setMonochrome,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MonoColors.Ink,
                        checkedTrackColor = MonoColors.Foreground,
                        uncheckedThumbColor = MonoColors.Muted,
                        uncheckedTrackColor = MonoColors.Graphite,
                        uncheckedBorderColor = MonoColors.Hairline,
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = viewModel::resetPosition) {
                    Text(stringResource(R.string.wallpaper_reset), color = MonoColors.Muted)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.apply()
                            Toast.makeText(context, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonoColors.Foreground,
                        contentColor = MonoColors.Ink,
                    ),
                ) {
                    Text(stringResource(R.string.wallpaper_apply), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Live preview with a miniature of the editorial home text; drag to move the focal point. */
@Composable
private fun WallpaperPreview(
    settings: WallpaperSettings,
    onBias: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val current by rememberUpdatedState(settings)
    val currentOnBias by rememberUpdatedState(onBias)

    Box(
        modifier
            .clip(PreviewShape)
            .border(1.dp, MonoColors.Hairline, PreviewShape)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val (ox, oy) = WallpaperMath.overflow(
                        imageSize.width,
                        imageSize.height,
                        size.width.toFloat(),
                        size.height.toFloat(),
                    )
                    currentOnBias(
                        WallpaperMath.dragBias(current.biasX, drag.x, ox),
                        WallpaperMath.dragBias(current.biasY, drag.y, oy),
                    )
                }
            },
    ) {
        WallpaperImage(settings, Modifier.fillMaxSize(), onImageSize = { imageSize = it })
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MonoColors.Ink.copy(alpha = 0.45f),
                        0.6f to Color.Transparent,
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp),
        ) {
            Text("November", color = MonoColors.Foreground, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("sunday", color = MonoColors.Foreground, fontFamily = Sacramento, fontSize = 18.sp)
            Text("fourthirty eight", color = MonoColors.Foreground, fontFamily = Outfit, fontWeight = FontWeight.Light, fontSize = 8.sp)
        }
    }
}

@Composable
private fun Thumbnail(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 56.dp, height = 104.dp)
                .clip(ThumbShape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MonoColors.Foreground else MonoColors.Hairline,
                    shape = ThumbShape,
                )
                .clickable(onClick = onClick),
        ) {
            content()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = if (selected) MonoColors.Foreground else MonoColors.Faint,
            fontSize = 11.sp,
        )
    }
}
