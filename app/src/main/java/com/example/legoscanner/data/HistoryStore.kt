package com.example.legoscanner.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    fun all(): List<ScanRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<ScanRecord>>(raw, TYPE)
        }.getOrDefault(emptyList()).sortedByDescending { it.timestamp }
    }

    fun save(record: ScanRecord) {
        val updated = (all().filterNot { it.id == record.id } + record)
            .sortedByDescending { it.timestamp }
            .take(MAX_RECORDS)

        prefs.edit().putString(KEY_RECORDS, gson.toJson(updated)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_RECORDS).apply()
    }

    private companion object {
        const val PREFS_NAME = "lego_scanner_history"
        const val KEY_RECORDS = "records"
        const val MAX_RECORDS = 300
        val TYPE = object : TypeToken<List<ScanRecord>>() {}.type
    }
}
