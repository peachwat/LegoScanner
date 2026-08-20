package com.example.legoscanner.data

import android.content.Context

class ProgressStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun found(setNum: String, partKey: String): Int =
        prefs.getInt(key(setNum, partKey), 0)

    fun add(setNum: String, partKey: String, delta: Int, limit: Int) {
        val current = found(setNum, partKey)
        val next = (current + delta).coerceIn(0, limit)
        prefs.edit().putInt(key(setNum, partKey), next).apply()
    }

    fun set(setNum: String, partKey: String, value: Int) {
        prefs.edit().putInt(key(setNum, partKey), value).apply()
    }

    fun clear(setNum: String) {
        val prefix = "$setNum|"
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith(prefix) }.forEach { remove(it) }
        }.apply()
    }

    private fun key(setNum: String, partKey: String) = "$setNum|$partKey"

    private companion object {
        const val PREFS_NAME = "lego_scanner_progress"
    }
}
