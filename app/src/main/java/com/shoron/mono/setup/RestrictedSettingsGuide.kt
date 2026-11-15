package com.shoron.mono.setup

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.shoron.mono.R
import com.shoron.mono.core.design.MonoChip
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.settings.SettingsNote
import com.shoron.mono.settings.SettingsSectionTitle
import com.shoron.mono.settings.SettingsTopBar

enum class RestrictedKind { Accessibility, NotificationAccess }

/**
 * Which app and which setting the guide is about. It can describe another app too: buttons
 * then open *that* app's App info and Settings pages, never MONO's.
 */
data class GuideTarget(
    val packageName: String,
    val appLabel: String,
    val serviceLabel: String,
    val kind: RestrictedKind,
    /** Exact service component when known (opens its own Settings page on Android 11+). */
    val component: ComponentName? = null,
)

/** Status as shown to the user. */
enum class GuideStatus { NotEnabled, WaitingForUser, Enabled }

object RestrictedGuideLogic {
    fun status(enabled: Boolean, userWentToSettings: Boolean): GuideStatus = when {
        enabled -> GuideStatus.Enabled
        userWentToSettings -> GuideStatus.WaitingForUser
        else -> GuideStatus.NotEnabled
    }
}

fun isTargetEnabled(context: Context, target: GuideTarget): Boolean = when (target.kind) {
    RestrictedKind.Accessibility -> {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        enabled.split(':').any { entry ->
            val cn = ComponentName.unflattenFromString(entry) ?: return@any false
            if (target.component != null) cn == target.component else cn.packageName == target.packageName
        }
    }
    RestrictedKind.NotificationAccess ->
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(target.packageName)
}

/**
 * Beginner-friendly guide for Android 13+ "Restricted settings" (Realme UI wording), with
 * live status. It only opens Settings screens; the user always makes the change.
 */
@Composable
fun RestrictedSettingsGuide(target: GuideTarget, onBack: () -> Unit) {
    val context = LocalContext.current
    var wentToSettings by rememberSaveable { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(isTargetEnabled(context, target)) }
    LifecycleResumeEffect(target) {
        enabled = isTargetEnabled(context, target)
        onPauseOrDispose { }
    }
    val status = RestrictedGuideLogic.status(enabled, wentToSettings)
    val settingName = stringResource(
        if (target.kind == RestrictedKind.Accessibility) R.string.guide_setting_accessibility else R.string.guide_setting_notifications,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 24.dp, bottom = 32.dp),
    ) {
        SettingsTopBar(stringResource(R.string.guide_title), onBack)
        Column(Modifier.padding(start = 12.dp)) {
            StatusPill(status)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.guide_for, target.serviceLabel, target.appLabel, settingName),
                color = MonoColors.Foreground,
                fontSize = 15.sp,
            )
            SettingsSectionTitle(stringResource(R.string.guide_why_title))
            SettingsNote(stringResource(R.string.guide_why_body))

            SettingsSectionTitle(stringResource(R.string.guide_steps_title))
            GuideStep(1, stringResource(R.string.guide_step1, target.appLabel, settingName)) { MockToggleBlocked(target.serviceLabel) }
            MonoChip(stringResource(R.string.guide_open_setting, settingName)) {
                wentToSettings = true
                openTargetSetting(context, target)
            }
            Spacer(Modifier.height(16.dp))
            GuideStep(2, stringResource(R.string.guide_step2, target.appLabel)) { MockAppInfoMenu(target.appLabel) }
            MonoChip(stringResource(R.string.guide_open_app_info, target.appLabel)) {
                wentToSettings = true
                SetupChecks.openAppInfo(context, target.packageName)
            }
            Spacer(Modifier.height(16.dp))
            GuideStep(3, stringResource(R.string.guide_step3)) { MockConfirm() }
            Spacer(Modifier.height(16.dp))
            GuideStep(4, stringResource(R.string.guide_step4, settingName, target.serviceLabel)) { MockToggleOn(target.serviceLabel) }
            MonoChip(stringResource(R.string.guide_open_setting, settingName), active = !enabled) {
                wentToSettings = true
                openTargetSetting(context, target)
            }

            SettingsSectionTitle(stringResource(R.string.guide_trouble_title))
            listOf(
                R.string.guide_trouble_menu,
                R.string.guide_trouble_old,
                R.string.guide_trouble_store,
                R.string.guide_trouble_realme,
                R.string.guide_trouble_reset,
            ).forEach { SettingsNote("• " + stringResource(it), Modifier.padding(bottom = 6.dp)) }
            Spacer(Modifier.height(8.dp))
            SettingsNote(stringResource(R.string.guide_security_note))
        }
    }
}

private fun openTargetSetting(context: Context, target: GuideTarget) {
    when (target.kind) {
        RestrictedKind.Accessibility -> SetupChecks.openAccessibilityFor(
            context,
            target.component ?: ComponentName(target.packageName, ""),
        )
        RestrictedKind.NotificationAccess -> SetupChecks.openNotificationAccessFor(
            context,
            target.component ?: ComponentName(target.packageName, ""),
        )
    }
}

@Composable
private fun StatusPill(status: GuideStatus) {
    val (label, active) = when (status) {
        GuideStatus.NotEnabled -> stringResource(R.string.guide_status_not_enabled) to false
        GuideStatus.WaitingForUser -> stringResource(R.string.guide_status_waiting) to false
        GuideStatus.Enabled -> stringResource(R.string.guide_status_enabled) to true
    }
    Text(
        label,
        color = if (active) MonoColors.Ink else MonoColors.Foreground,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) MonoColors.Foreground else MonoColors.Graphite)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun GuideStep(number: Int, text: String, illustration: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(24.dp)
                .border(1.dp, MonoColors.Foreground, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("$number", color = MonoColors.Foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = MonoColors.Foreground, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            illustration()
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ------------------------------------------------------------------ simplified screen mock-ups

private val MockShape = RoundedCornerShape(12.dp)

@Composable
private fun MockFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MockShape)
            .background(MonoColors.Charcoal)
            .border(1.dp, MonoColors.Hairline, MockShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun MockToggleBlocked(service: String) = MockFrame {
    MockRow(service, toggle = false)
    Text(stringResource(R.string.guide_mock_restricted), color = MonoColors.Muted, fontSize = 12.sp)
}

@Composable
private fun MockAppInfoMenu(app: String) = MockFrame {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.guide_mock_app_info, app), color = MonoColors.Foreground, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            "⋮",
            color = MonoColors.Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .background(MonoColors.Foreground)
                .padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
    Text(
        stringResource(R.string.guide_mock_allow),
        color = MonoColors.Foreground,
        fontSize = 13.sp,
        modifier = Modifier
            .align(Alignment.End)
            .border(1.dp, MonoColors.Foreground, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun MockConfirm() = MockFrame {
    Text(stringResource(R.string.guide_mock_confirm), color = MonoColors.Muted, fontSize = 12.sp)
}

@Composable
private fun MockToggleOn(service: String) = MockFrame { MockRow(service, toggle = true) }

@Composable
private fun MockRow(label: String, toggle: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MonoColors.Foreground, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(width = 34.dp, height = 18.dp)
                .clip(RoundedCornerShape(50))
                .background(if (toggle) MonoColors.Foreground else MonoColors.Graphite),
            contentAlignment = if (toggle) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(2.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (toggle) MonoColors.Ink else MonoColors.Faint),
            )
        }
    }
}
