package com.shoron.mono.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoron.mono.MonoApplication
import com.shoron.mono.R
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppIcon
import com.shoron.mono.apps.AppRepository
import com.shoron.mono.core.design.MonoChip
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import com.shoron.mono.data.HomeLayout
import com.shoron.mono.data.LauncherPreferences
import com.shoron.mono.icons.IconRepository
import com.shoron.mono.icons.MissingIcon
import com.shoron.mono.icons.MissingIconsExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class IconFilter { All, User, System }

/** A missing-icon row: the launchable app plus its export data. */
data class MissingRow(val entry: AppEntry, val info: MissingIcon)

class IconsViewModel(
    appRepository: AppRepository,
    iconRepository: IconRepository,
    private val preferences: LauncherPreferences,
    private val packageManager: PackageManager,
) : ViewModel() {

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(IconFilter.All)
    val selected = MutableStateFlow<Set<String>>(emptySet())

    val layout: StateFlow<HomeLayout?> = preferences.homeLayout.map<HomeLayout, HomeLayout?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val supportedCount: StateFlow<Int> = iconRepository.themedIcons.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Every launchable app without a Thin Line icon (missing-icon detection). */
    private val missing: StateFlow<List<MissingRow>> =
        combine(appRepository.apps, iconRepository.themedIcons) { apps, themed ->
            val systemCache = HashMap<String, Boolean>()
            apps.orEmpty().filter { it.key !in themed }.map { app ->
                val system = systemCache.getOrPut(app.packageName) { isSystem(app.packageName) }
                MissingRow(app, MissingIcon(app.label, app.packageName, app.className, system))
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val missingCount: StateFlow<Int> = missing.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Missing apps after search and filter. */
    val visible: StateFlow<List<MissingRow>> = combine(missing, query, filter) { rows, q, f ->
        val needle = q.trim().lowercase()
        rows.filter { row ->
            (f == IconFilter.All || (f == IconFilter.System) == row.info.system) &&
                (needle.isEmpty() || row.info.label.lowercase().contains(needle) ||
                    row.info.packageName.lowercase().contains(needle))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setThinLine(on: Boolean) {
        viewModelScope.launch { preferences.updateHomeLayout { it.copy(thinLineIcons = on) } }
    }

    fun toggle(key: String) = selected.update { if (key in it) it - key else it + key }

    fun selectAllVisible() = selected.update { it + visible.value.map { row -> row.entry.key } }

    fun clearSelection() {
        selected.value = emptySet()
    }

    /** Selected rows if any are selected, otherwise every visible row. */
    fun exportRows(): List<MissingIcon> {
        val sel = selected.value
        val rows = visible.value
        return (if (sel.isEmpty()) rows else rows.filter { it.entry.key in sel }).map { it.info }
    }

    private fun isSystem(pkg: String): Boolean = runCatching {
        val flags = packageManager.getApplicationInfo(pkg, 0).flags
        flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }.getOrDefault(false)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                IconsViewModel(
                    app.container.appRepository,
                    app.container.iconRepository,
                    app.container.launcherPreferences,
                    app.packageManager,
                )
            }
        }
    }
}

/** Thin Line icon coverage: counts, toggle, searchable missing list, copy and export. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconsScreen(onBack: () -> Unit) {
    val viewModel: IconsViewModel = viewModel(factory = IconsViewModel.Factory)
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val supported by viewModel.supportedCount.collectAsStateWithLifecycle()
    val missingCount by viewModel.missingCount.collectAsStateWithLifecycle()
    val rows by viewModel.visible.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun export(uri: Uri?, format: (List<MissingIcon>) -> String) {
        if (uri == null) return
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(format(viewModel.exportRows()).toByteArray()) }
        }.isSuccess
        Toast.makeText(context, if (ok) R.string.icons_exported else R.string.icons_export_failed, Toast.LENGTH_SHORT).show()
    }
    // System "save as" dialogs: no storage permission needed.
    val saveTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { export(it, MissingIconsExport::txt) }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { export(it, MissingIconsExport::csv) }
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { export(it, MissingIconsExport::json) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 12.dp, end = 20.dp, bottom = 32.dp),
    ) {
        item { SettingsTopBar(stringResource(R.string.icons_title), onBack) }
        item {
            Column(Modifier.padding(start = 12.dp)) {
                SwitchRow(stringResource(R.string.icons_thin_line), layout?.thinLineIcons ?: true, viewModel::setThinLine)
                Text(
                    stringResource(R.string.icons_counts, supported, missingCount),
                    color = MonoColors.Foreground,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                SettingsSectionTitle(stringResource(R.string.icons_missing))
                BasicTextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    singleLine = true,
                    textStyle = TextStyle(color = MonoColors.Foreground, fontSize = 15.sp, fontFamily = Outfit),
                    cursorBrush = SolidColor(MonoColors.Foreground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MonoColors.Charcoal)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text(stringResource(R.string.icons_search_hint), color = MonoColors.Faint, fontSize = 15.sp)
                        inner()
                    },
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonoChip(stringResource(R.string.icons_filter_all), active = filter == IconFilter.All) { viewModel.filter.value = IconFilter.All }
                    MonoChip(stringResource(R.string.icons_filter_user), active = filter == IconFilter.User) { viewModel.filter.value = IconFilter.User }
                    MonoChip(stringResource(R.string.icons_filter_system), active = filter == IconFilter.System) { viewModel.filter.value = IconFilter.System }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.icons_selection, selected.size, rows.size),
                    color = MonoColors.Muted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonoChip(stringResource(R.string.icons_select_all)) { viewModel.selectAllVisible() }
                    MonoChip(stringResource(R.string.icons_clear)) { viewModel.clearSelection() }
                    MonoChip(stringResource(R.string.icons_copy_names)) {
                        copy(context, MissingIconsExport.names(viewModel.exportRows()))
                    }
                    MonoChip(stringResource(R.string.icons_copy_packages)) {
                        copy(context, MissingIconsExport.packages(viewModel.exportRows()))
                    }
                    MonoChip(stringResource(R.string.icons_copy_both)) {
                        copy(context, MissingIconsExport.txt(viewModel.exportRows()))
                    }
                    MonoChip("TXT") { saveTxt.launch("mono-missing-icons.txt") }
                    MonoChip("CSV") { saveCsv.launch("mono-missing-icons.csv") }
                    MonoChip("JSON") { saveJson.launch("mono-missing-icons.json") }
                }
                Spacer(Modifier.height(6.dp))
                SettingsNote(stringResource(R.string.icons_actions_note))
                Spacer(Modifier.height(8.dp))
            }
        }
        items(rows, key = { it.entry.key }) { row ->
            val isSelected = row.entry.key in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggle(row.entry.key) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { viewModel.toggle(row.entry.key) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MonoColors.Foreground,
                        uncheckedColor = MonoColors.Faint,
                        checkmarkColor = MonoColors.Ink,
                    ),
                )
                AppIcon(row.entry, 26.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.info.label, color = MonoColors.Foreground, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        row.info.packageName + if (row.info.system) " · system" else "",
                        color = MonoColors.Faint,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    stringResource(R.string.icons_copy),
                    color = MonoColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { copy(context, "${row.info.label}\t${row.info.packageName}") }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    if (text.isEmpty()) return
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("MONO missing icons", text))
    Toast.makeText(context, R.string.icons_copied, Toast.LENGTH_SHORT).show()
}
