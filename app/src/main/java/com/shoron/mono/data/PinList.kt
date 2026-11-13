package com.shoron.mono.data

enum class PinResult { Pinned, Unpinned, Full }

/** Pure operations on the ordered list of pinned app keys. */
object PinList {

    const val MAX_PINS = 8

    fun encode(keys: List<String>): String = keys.joinToString("\n")

    fun decode(raw: String): List<String> =
        raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun toggle(keys: List<String>, key: String, max: Int = MAX_PINS): Pair<List<String>, PinResult> =
        when {
            key in keys -> (keys - key) to PinResult.Unpinned
            keys.size >= max -> keys to PinResult.Full
            else -> (keys + key) to PinResult.Pinned
        }

    fun move(keys: List<String>, key: String, delta: Int): List<String> {
        val from = keys.indexOf(key)
        if (from < 0) return keys
        val to = (from + delta).coerceIn(0, keys.lastIndex)
        if (from == to) return keys
        return keys.toMutableList().apply { add(to, removeAt(from)) }
    }
}
