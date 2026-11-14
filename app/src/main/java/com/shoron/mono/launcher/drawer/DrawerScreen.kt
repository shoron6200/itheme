package com.shoron.mono.launcher.drawer

import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.shoron.mono.core.design.LaunchBounds
import com.shoron.mono.core.design.pressScale
import com.shoron.mono.core.design.trackBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.R
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppIcon
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import com.shoron.mono.launcher.AppAction
import com.shoron.mono.launcher.AppActionsMenu
import kotlinx.coroutines.launch

@Composable
fun DrawerScreen(
    state: DrawerUiState,
    recents: List<AppEntry>,
    focusSearchToken: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentOnClose by rememberUpdatedState(onClose)
    val closeThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    val pullToClose = remember(closeThresholdPx) {
        PullToCloseConnection(closeThresholdPx) { currentOnClose() }
    }

    LaunchedEffect(query) { listState.scrollToItem(0) }

    // Swipe-down "Search" opens the drawer with the keyboard ready.
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusSearchToken) {
        if (focusSearchToken > 0) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MonoColors.Ink.copy(alpha = 0.93f))
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = { state.firstApp?.let { onLaunch(it, null) } },
            modifier = Modifier
                .padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 8.dp)
                .focusRequester(focusRequester),
        )
        AnimatedVisibility(visible = query.isBlank() && recents.isNotEmpty()) {
            RecentApps(recents, onLaunch)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToClose),
                contentPadding = PaddingValues(start = 32.dp, end = 56.dp, bottom = 24.dp),
            ) {
                items(
                    items = state.rows,
                    key = { it.rowKey },
                    contentType = { if (it is DrawerRow.Header) 0 else 1 },
                ) { row ->
                    when (row) {
                        is DrawerRow.Header -> SectionHeader(row.letter)
                        is DrawerRow.App -> DrawerAppRow(
                            entry = row.entry,
                            isPinned = row.entry.key in state.pinnedKeys,
                            onLaunch = onLaunch,
                            onTogglePin = onTogglePin,
                            onAppInfo = onAppInfo,
                        )
                    }
                }
            }

            if (query.isBlank() && state.letters.size > 1) {
                // Section at the top of the list; read lazily so scrolling only recomposes the rail.
                val currentLetter by remember(state.letterIndex) {
                    derivedStateOf {
                        val first = listState.firstVisibleItemIndex
                        state.letterIndex.entries.lastOrNull { it.value <= first }?.key
                    }
                }
                AlphabetRail(
                    letters = state.letters,
                    current = currentLetter,
                    onLetter = { letter, smooth ->
                        state.letterIndex[letter]?.let { index ->
                            scope.launch {
                                if (smooth) listState.animateScrollToItem(index) else listState.scrollToItem(index)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.85f)
                        .width(44.dp)
                        .padding(end = 6.dp),
                )
            }

            if (!state.isLoading && state.rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.drawer_empty),
                    color = MonoColors.Faint,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = MonoColors.Foreground, fontSize = 16.sp, fontFamily = Outfit),
        cursorBrush = SolidColor(MonoColors.Foreground),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MonoColors.Faint,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.drawer_search_hint),
                            color = MonoColors.Faint,
                            fontSize = 16.sp,
                        )
                    }
                    inner()
                }
            }
        },
    )
}

/** Apps recently launched from MONO (most recent first). */
@Composable
private fun RecentApps(recents: List<AppEntry>, onLaunch: (AppEntry, Rect?) -> Unit) {
    Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.drawer_recent),
            color = MonoColors.Faint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(recents, key = { it.key }) { entry ->
                val interaction = remember { MutableInteractionSource() }
                val bounds = remember { LaunchBounds() }
                Column(
                    Modifier
                        .width(60.dp)
                        .trackBounds(bounds)
                        .pressScale(interaction)
                        .clickable(interactionSource = interaction, indication = null) { onLaunch(entry, bounds.rect) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcon(entry, 32.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = entry.label,
                        color = MonoColors.Muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(letter: String) {
    Text(
        text = letter,
        color = MonoColors.Faint,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppRow(
    entry: AppEntry,
    isPinned: Boolean,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val bounds = remember { LaunchBounds() }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .trackBounds(bounds)
                .pressScale(interaction, 0.97f)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onLaunch(entry, bounds.rect) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(entry, 28.dp)
            Spacer(Modifier.width(20.dp))
            Text(
                text = entry.label,
                color = MonoColors.Foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (entry.isWorkProfile) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.drawer_work_badge),
                    color = MonoColors.Faint,
                    fontSize = 10.sp,
                )
            }
        }
        AppActionsMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            actions = listOf(
                AppAction(
                    stringResource(if (isPinned) R.string.action_unpin else R.string.action_pin),
                ) { onTogglePin(entry) },
                AppAction(stringResource(R.string.action_app_info)) { onAppInfo(entry) },
            ),
        )
    }
}

/**
 * Closes the drawer when the user keeps dragging down after the list is already at the top.
 * Fling momentum is ignored so a fast scroll to the top never closes the drawer by accident.
 */
private class PullToCloseConnection(
    private val thresholdPx: Float,
    private val onClose: () -> Unit,
) : NestedScrollConnection {
    private var pulled = 0f
    private var flinging = false

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (flinging) return Offset.Zero
        if (available.y > 0f) {
            pulled += available.y
            if (pulled >= thresholdPx) {
                pulled = 0f
                onClose()
            }
        } else if (consumed.y != 0f) {
            pulled = 0f
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        flinging = true
        pulled = 0f
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        flinging = false
        return Velocity.Zero
    }
}
