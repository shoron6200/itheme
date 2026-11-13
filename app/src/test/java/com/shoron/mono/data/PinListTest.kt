package com.shoron.mono.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PinListTest {

    @Test fun encodeDecodeRoundTrip() {
        val keys = listOf("a/b|0", "c/d|10")
        assertEquals(keys, PinList.decode(PinList.encode(keys)))
        assertEquals(emptyList<String>(), PinList.decode(""))
        assertEquals(listOf("x"), PinList.decode("x\n\nx\n"))
    }

    @Test fun togglePinsUnpinsAndRespectsLimit() {
        assertEquals(listOf("a", "b") to PinResult.Pinned, PinList.toggle(listOf("a"), "b", max = 2))
        assertEquals(listOf("b") to PinResult.Unpinned, PinList.toggle(listOf("a", "b"), "a", max = 2))
        assertEquals(listOf("a", "b") to PinResult.Full, PinList.toggle(listOf("a", "b"), "c", max = 2))
    }

    @Test fun moveClampsToBounds() {
        val keys = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), PinList.move(keys, "b", -1))
        assertEquals(listOf("a", "c", "b"), PinList.move(keys, "b", 1))
        assertEquals(keys, PinList.move(keys, "a", -1))
        assertEquals(keys, PinList.move(keys, "missing", 1))
    }
}
