package com.shoron.mono.widgets.clock

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Display-ready clock values; equality-based so the UI recomposes only when text changes. */
@Immutable
data class ClockSnapshot(
    val month: String,
    val dayOfWeek: String,
    val hourWord: String,
    val minuteWords: String,
    val dateLine: String,
    /** Lock-screen vertical date, e.g. "02-NOVEMBER". */
    val dayMonth: String,
    /** Short weekday, e.g. "SUN". */
    val dayShort: String,
) {
    companion object {
        fun from(time: LocalDateTime, locale: Locale = Locale.getDefault()): ClockSnapshot {
            val month = time.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                .replaceFirstChar { it.titlecase(locale) }
            val day = time.dayOfWeek.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
            val shortDay = time.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
            val shortMonth = time.month.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
            return ClockSnapshot(
                month = month,
                dayOfWeek = day,
                hourWord = TimeInWords.hour(time.hour),
                minuteWords = TimeInWords.minutes(time.minute),
                dateLine = "$shortDay  ${time.dayOfMonth} $shortMonth ${time.year}",
                dayMonth = time.dayOfMonth.toString().padStart(2, '0') + "-" + month.uppercase(locale),
                dayShort = shortDay,
            )
        }
    }
}
