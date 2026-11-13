package com.shoron.mono.widgets.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatterTest {

    @Test fun twentyFourHourIsZeroPadded() {
        assertEquals("00", ClockFormatter.hour(0, use24Hour = true))
        assertEquals("04", ClockFormatter.hour(4, use24Hour = true))
        assertEquals("16", ClockFormatter.hour(16, use24Hour = true))
        assertEquals("23", ClockFormatter.hour(23, use24Hour = true))
    }

    @Test fun twelveHourHandlesMidnightAndNoon() {
        assertEquals("12", ClockFormatter.hour(0, use24Hour = false))
        assertEquals("12", ClockFormatter.hour(12, use24Hour = false))
        assertEquals("4", ClockFormatter.hour(16, use24Hour = false))
        assertEquals("11", ClockFormatter.hour(23, use24Hour = false))
        assertEquals("AM", ClockFormatter.amPm(0))
        assertEquals("PM", ClockFormatter.amPm(12))
    }

    @Test fun twoDigits() {
        assertEquals("00", ClockFormatter.twoDigits(0))
        assertEquals("07", ClockFormatter.twoDigits(7))
        assertEquals("59", ClockFormatter.twoDigits(59))
    }
}
