package com.shoron.mono.widgets.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryStateTest {

    @Test fun percentFromLevelAndScale() {
        assertEquals(75, BatteryState.from(level = 75, scale = 100, status = 3, plugged = 0).percent)
        assertEquals(50, BatteryState.from(level = 128, scale = 255, status = 3, plugged = 0).percent)
        assertEquals(0, BatteryState.from(level = -1, scale = 100, status = 1, plugged = 0).percent)
    }

    @Test fun chargingStatusAndSource() {
        val usb = BatteryState.from(60, 100, BatteryState.STATUS_CHARGING, BatteryState.PLUGGED_USB)
        assertTrue(usb.charging)
        assertEquals(PowerSource.USB, usb.source)

        val unplugged = BatteryState.from(60, 100, 3, 0)
        assertFalse(unplugged.charging)
        assertEquals(PowerSource.None, unplugged.source)

        val full = BatteryState.from(100, 100, BatteryState.STATUS_FULL, BatteryState.PLUGGED_AC)
        assertTrue(full.full)
        assertFalse(full.charging)
        assertEquals(PowerSource.AC, full.source)

        // Plugged in but status not yet updated still counts as charging.
        assertTrue(BatteryState.from(40, 100, 3, BatteryState.PLUGGED_WIRELESS).charging)
    }
}
