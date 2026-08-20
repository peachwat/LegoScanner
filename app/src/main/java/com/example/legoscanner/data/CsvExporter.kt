package com.example.legoscanner.data

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private const val EXPORT_DIR = "exports"
    private const val FILE_NAME = "lego_scanner_history.csv"

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun export(context: Context, records: List<ScanRecord>): android.net.Uri {
        val csv = buildCsv(records)

        val directory = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        val file = File(directory, FILE_NAME).apply { writeText(csv, Charsets.UTF_8) }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun buildCsv(records: List<ScanRecord>): String = buildString {
        appendLine(
            listOf(
                "scan_id", "timestamp", "set_num",
                "part_num", "part_name", "color_name",
                "confidence", "color_ambiguous", "status",
                "corrected_to_part", "corrected_to_color",
                "scan_detected", "scan_counted", "scan_model_errors",
                "scan_rejected_not_in_set", "scan_rejected_low_confidence",
                "scan_avg_confidence"
            ).joinToString(";")
        )

        records.forEach { record ->
            val time = timestampFormat.format(Date(record.timestamp))

            if (record.entries.isEmpty()) {
                appendLine(
                    listOf(
                        record.id, time, record.setNum,
                        "", "", "", "", "", "NO_DETECTIONS", "", "",
                        0, 0, 0,
                        record.rejectedNotInSet, record.rejectedLowConfidence,
                        format(0f)
                    ).joinToString(";")
                )
                return@forEach
            }

            record.entries.forEach { entry ->
                appendLine(
                    listOf(
                        record.id,
                        time,
                        record.setNum,
                        entry.partNum,
                        escape(entry.partName),
                        escape(entry.colorName),
                        format(entry.confidence),
                        entry.colorAmbiguous,
                        entry.status,
                        entry.correctedToPartNum.orEmpty(),
                        escape(entry.correctedToColorName.orEmpty()),
                        record.detected,
                        record.counted,
                        record.modelErrors,
                        record.rejectedNotInSet,
                        record.rejectedLowConfidence,
                        format(record.averageConfidence)
                    ).joinToString(";")
                )
            }
        }
    }

    private fun escape(value: String): String = value.replace(';', ',').replace('\n', ' ')

    private fun format(value: Float): String = String.format(Locale.US, "%.4f", value)
}
