package com.shoron.mono.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoron.mono.R
import com.shoron.mono.core.design.MonoChip
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.monoContainer
import com.shoron.mono.settings.SettingsNote
import com.shoron.mono.settings.SettingsTopBar

private val CardShape = RoundedCornerShape(16.dp)

/**
 * First-launch setup assistant (and Settings → Permission setup). Every step explains why it is
 * needed, opens the right Android Settings screen, and re-checks the real status when the user
 * comes back. Optional steps can be skipped; nothing is granted automatically.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    onboarding: Boolean,
    onDone: () -> Unit,
    onOpenGuide: (SetupStepId) -> Unit,
    onOpenWeather: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { SetupStore(context) }
    val widgets by context.monoContainer.widgetPreferences.widgets.collectAsStateWithLifecycle(initialValue = null)
    val weatherConfigured = widgets?.weatherPlace != null
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++ // re-check every permission when the user returns from Android Settings
        onPauseOrDispose { }
    }

    val steps = remember(refresh, weatherConfigured) {
        SetupStepId.entries.map { id ->
            StepStatus(
                id = id,
                required = SetupChecks.isRequired(id),
                state = SetupProgress.state(
                    granted = SetupChecks.isGranted(context, id, weatherConfigured),
                    opened = store.wasOpened(id),
                    skipped = store.isSkipped(id),
                ),
            )
        }
    }
    val requiredDone = steps.filter { it.required }.all { it.state == StepState.Done }
    // Shown once: leaving the assistant any way (Finish, Later, Back) never re-opens it on its
    // own; it stays available under Settings → Permission setup.
    LaunchedEffect(onboarding) { if (onboarding) store.onboardingDone = true }
    val finish = {
        store.onboardingDone = true
        onDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 20.dp, bottom = 32.dp),
    ) {
        SettingsTopBar(stringResource(if (onboarding) R.string.setup_welcome else R.string.setup_title), finish)
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(R.string.setup_intro), color = MonoColors.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { SetupProgress.progress(steps) },
                color = MonoColors.Foreground,
                trackColor = MonoColors.Hairline,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    R.string.setup_progress,
                    steps.count { it.state == StepState.Done || it.state == StepState.Skipped },
                    steps.size,
                ),
                color = MonoColors.Faint,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(16.dp))

            steps.forEach { step ->
                StepCard(
                    step = step,
                    onOpen = {
                        if (step.id == SetupStepId.Weather) {
                            onOpenWeather()
                        } else {
                            store.markOpened(step.id)
                            SetupChecks.open(context, step.id)
                        }
                        refresh++
                    },
                    onSkip = {
                        store.setSkipped(step.id, step.state != StepState.Skipped)
                        refresh++
                    },
                    onGuide = if (
                        (step.id == SetupStepId.Accessibility || step.id == SetupStepId.Notifications) &&
                        SetupChecks.restrictedSettingsMayApply(context)
                    ) {
                        { onOpenGuide(step.id) }
                    } else {
                        null
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))
            if (onboarding) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonoChip(stringResource(R.string.setup_finish), active = requiredDone, onClick = finish)
                    MonoChip(stringResource(R.string.setup_later), onClick = finish)
                }
                Spacer(Modifier.height(8.dp))
                SettingsNote(stringResource(R.string.setup_revisit))
            } else if (SetupProgress.isComplete(steps)) {
                SettingsNote(stringResource(R.string.setup_all_done))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepCard(step: StepStatus, onOpen: () -> Unit, onSkip: () -> Unit, onGuide: (() -> Unit)?) {
    val (title, why) = when (step.id) {
        SetupStepId.Home -> R.string.setup_home_title to R.string.setup_home_why
        SetupStepId.Overlay -> R.string.setup_overlay_title to R.string.setup_overlay_why
        SetupStepId.Accessibility -> R.string.setup_access_title to R.string.setup_access_why
        SetupStepId.Notifications -> R.string.setup_notif_title to R.string.setup_notif_why
        SetupStepId.Battery -> R.string.setup_battery_title to R.string.setup_battery_why
        SetupStepId.Weather -> R.string.setup_weather_title to R.string.setup_weather_why
    }
    val done = step.state == StepState.Done
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MonoColors.Charcoal)
            .border(1.dp, if (done) MonoColors.Hairline else MonoColors.Muted, CardShape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(title),
                color = MonoColors.Foreground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(
                    when (step.state) {
                        StepState.Done -> R.string.setup_state_done
                        StepState.Waiting -> R.string.setup_state_waiting
                        StepState.Skipped -> R.string.setup_state_skipped
                        StepState.NotEnabled -> if (step.required) R.string.setup_state_required else R.string.setup_state_optional
                    },
                ),
                color = if (done) MonoColors.Foreground else MonoColors.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(why), color = MonoColors.Muted, fontSize = 13.sp)
        if (!done) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoChip(stringResource(R.string.setup_open), active = true, onClick = onOpen)
                if (onGuide != null) MonoChip(stringResource(R.string.setup_restricted_help), onClick = onGuide)
                if (!step.required) {
                    MonoChip(
                        stringResource(if (step.state == StepState.Skipped) R.string.setup_unskip else R.string.setup_skip),
                        onClick = onSkip,
                    )
                }
            }
        }
    }
}
