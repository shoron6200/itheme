package com.shoron.mono.widgets.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeInWordsTest {

    @Test fun hoursUseTwelveHourWords() {
        assertEquals("twelve", TimeInWords.hour(0))
        assertEquals("one", TimeInWords.hour(1))
        assertEquals("twelve", TimeInWords.hour(12))
        assertEquals("four", TimeInWords.hour(16))
        assertEquals("eleven", TimeInWords.hour(23))
    }

    @Test fun minutesReadNaturally() {
        assertEquals("o'clock", TimeInWords.minutes(0))
        assertEquals("oh five", TimeInWords.minutes(5))
        assertEquals("ten", TimeInWords.minutes(10))
        assertEquals("fifteen", TimeInWords.minutes(15))
        assertEquals("twenty", TimeInWords.minutes(20))
        assertEquals("thirty eight", TimeInWords.minutes(38))
        assertEquals("fifty nine", TimeInWords.minutes(59))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidMinute() {
        TimeInWords.minutes(60)
    }
}
