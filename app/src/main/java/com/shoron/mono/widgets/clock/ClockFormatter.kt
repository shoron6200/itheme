package com.shoron.mono.widgets.clock

/** Pure digital-clock formatting (JVM-tested). */
object ClockFormatter {

    /** "16" / "04" in 24-hour mode; "4" / "12" in 12-hour mode. */
    fun hour(hour24: Int, use24Hour: Boolean): String {
        val h = Math.floorMod(hour24, 24)
        return if (use24Hour) h.toString().padStart(2, '0') else (if (h % 12 == 0) 12 else h % 12).toString()
    }

    fun twoDigits(value: Int): String = Math.floorMod(value, 60).toString().padStart(2, '0')

    fun amPm(hour24: Int): String = if (Math.floorMod(hour24, 24) < 12) "AM" else "PM"
}
