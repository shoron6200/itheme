package com.shoron.mono.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoron.mono.R
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import com.shoron.mono.data.ClockStyle
import com.shoron.mono.data.TemperatureUnit

/** Clock style, time format, seconds, battery and weather settings (persisted in DataStore). */
@Composable
fun WidgetsScreen(onBack: () -> Unit) {
    val viewModel: WidgetSettingsViewModel = viewModel(factory = WidgetSettingsViewModel.Factory)
    val widgets by viewModel.widgets.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val settings = widgets ?: return

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 12.dp, end = 24.dp, bottom = 32.dp),
    ) {
        item { SettingsTopBar(stringResource(R.string.settings_widgets), onBack) }

        item {
            Column(Modifier.padding(start = 12.dp)) {
                SettingsSectionTitle(stringResource(R.string.widgets_clock_style))
                RadioRow(
                    stringResource(R.string.widgets_clock_radial),
                    settings.clockStyle == ClockStyle.RadialDigital,
                ) { viewModel.updateWidgets { it.copy(clockStyle = ClockStyle.RadialDigital) } }
                RadioRow(
                    stringResource(R.string.widgets_clock_words),
                    settings.clockStyle == ClockStyle.EditorialWords,
                ) { viewModel.updateWidgets { it.copy(clockStyle = ClockStyle.EditorialWords) } }
                Spacer(Modifier.height(8.dp))
                SwitchRow(stringResource(R.string.widgets_24h), settings.use24Hour) { on ->
                    viewModel.updateWidgets { it.copy(use24Hour = on) }
                }
                SwitchRow(stringResource(R.string.widgets_seconds), settings.showSeconds) { on ->
                    viewModel.updateWidgets { it.copy(showSeconds = on) }
                }
                SwitchRow(stringResource(R.string.widgets_battery), settings.showBattery) { on ->
                    viewModel.updateWidgets { it.copy(showBattery = on) }
                }

                SettingsSectionTitle(stringResource(R.string.widgets_weather))
                SwitchRow(stringResource(R.string.widgets_weather_enabled), settings.weatherEnabled) { on ->
                    viewModel.updateWidgets { it.copy(weatherEnabled = on) }
                }
                SwitchRow(stringResource(R.string.widgets_forecast), settings.showForecast) { on ->
                    viewModel.updateWidgets { it.copy(showForecast = on) }
                }
                SwitchRow(
                    stringResource(R.string.widgets_fahrenheit),
                    settings.temperatureUnit == TemperatureUnit.Fahrenheit,
                ) { on ->
                    viewModel.updateWidgets {
                        it.copy(temperatureUnit = if (on) TemperatureUnit.Fahrenheit else TemperatureUnit.Celsius)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.widgets_location), color = MonoColors.Muted, fontSize = 13.sp)
                Text(
                    text = settings.weatherPlace?.let { listOf(it.name, it.region).filter(String::isNotBlank).joinToString(", ") }
                        ?: stringResource(R.string.widgets_location_none),
                    color = MonoColors.Foreground,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = MonoColors.Foreground, fontSize = 15.sp, fontFamily = Outfit),
                        cursorBrush = SolidColor(MonoColors.Foreground),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
                        modifier = Modifier
                            .weight(1f)
                            .background(MonoColors.Charcoal)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(stringResource(R.string.widgets_search_hint), color = MonoColors.Faint, fontSize = 15.sp)
                            }
                            inner()
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    if (searching) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = MonoColors.Foreground, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { viewModel.search(query) }, enabled = query.isNotBlank()) {
                            Text(stringResource(R.string.widgets_search), color = MonoColors.Foreground)
                        }
                    }
                }
                if (results?.isEmpty() == true) {
                    SettingsNote(stringResource(R.string.widgets_no_results), Modifier.padding(top = 8.dp))
                }
            }
        }

        items(results.orEmpty(), key = { "${it.latitude},${it.longitude}" }) { place ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.choosePlace(place)
                        query = ""
                    }
                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Text(place.name, color = MonoColors.Foreground, fontSize = 15.sp)
                if (place.region.isNotBlank()) Text(place.region, color = MonoColors.Faint, fontSize = 12.sp)
            }
            HorizontalDivider(Modifier.padding(start = 12.dp), color = MonoColors.Hairline)
        }

        item {
            SettingsNote(stringResource(R.string.widgets_weather_source), Modifier.padding(start = 12.dp, top = 20.dp))
        }
    }
}
