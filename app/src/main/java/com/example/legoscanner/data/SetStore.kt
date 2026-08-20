package com.example.legoscanner.data

import android.content.Context
import com.example.legoscanner.Config

class SetStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var setNum: String
        get() = prefs.getString(KEY_SET_NUM, null) ?: Config.DEFAULT_SET_NUM
        set(value) = prefs.edit().putString(KEY_SET_NUM, value).apply()

    companion object {
        private const val PREFS_NAME = "lego_scanner_prefs"
        private const val KEY_SET_NUM = "last_set_num"

        fun normalize(input: String): String {
            val trimmed = input.trim()
            return if (trimmed.contains('-')) trimmed else "$trimmed-1"
        }
    }
}
