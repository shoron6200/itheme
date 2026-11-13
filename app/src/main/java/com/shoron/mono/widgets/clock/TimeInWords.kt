package com.shoron.mono.widgets.clock

/** English time-in-words, e.g. 16:38 -> "four" + "thirty eight". */
object TimeInWords {

    private val ones = arrayOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen",
    )
    private val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty")

    fun hour(hour24: Int): String {
        val h = Math.floorMod(hour24, 12)
        return ones[if (h == 0) 12 else h]
    }

    fun minutes(minute: Int): String {
        require(minute in 0..59) { "minute out of range: $minute" }
        return when {
            minute == 0 -> "o'clock"
            minute < 10 -> "oh ${ones[minute]}"
            minute < 20 -> ones[minute]
            minute % 10 == 0 -> tens[minute / 10]
            else -> "${tens[minute / 10]} ${ones[minute % 10]}"
        }
    }
}
