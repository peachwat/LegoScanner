package com.example.legoscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.legoscanner.data.HistoryStore
import com.example.legoscanner.data.ScanRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistorySummary(
    val scans: Int,
    val detected: Int,
    val counted: Int,
    val modelErrors: Int,
    val averageConfidence: Float
) {
    val errorRate: Float get() = if (detected == 0) 0f else modelErrors.toFloat() / detected
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyStore = HistoryStore(application)

    private val _records = MutableStateFlow<List<ScanRecord>>(emptyList())
    val records: StateFlow<List<ScanRecord>> = _records.asStateFlow()

    fun refresh() {
        _records.value = historyStore.all()
    }

    fun clear() {
        historyStore.clear()
        refresh()
    }

    fun summary(): HistorySummary {
        val all = _records.value
        val detected = all.sumOf { it.detected }

        return HistorySummary(
            scans = all.size,
            detected = detected,
            counted = all.sumOf { it.counted },
            modelErrors = all.sumOf { it.modelErrors },
            averageConfidence = all.flatMap { it.entries }
                .map { it.confidence }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toFloat() ?: 0f
        )
    }
}
