package com.example.legoscanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.legoscanner.data.CsvExporter
import com.example.legoscanner.data.ScanRecord
import com.example.legoscanner.ui.HistoryAdapter
import com.example.legoscanner.ui.HistoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val viewModel: HistoryViewModel by viewModels()
    private val adapter = HistoryAdapter { showDetails(it) }

    private lateinit var historyList: RecyclerView
    private lateinit var emptyHistory: TextView
    private lateinit var summaryStats: TextView
    private lateinit var exportButton: Button
    private lateinit var clearButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyList = view.findViewById(R.id.historyList)
        emptyHistory = view.findViewById(R.id.emptyHistory)
        summaryStats = view.findViewById(R.id.summaryStats)
        exportButton = view.findViewById(R.id.exportButton)
        clearButton = view.findViewById(R.id.clearHistoryButton)

        historyList.layoutManager = LinearLayoutManager(requireContext())
        historyList.adapter = adapter
        historyList.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        exportButton.setOnClickListener { exportCsv() }
        clearButton.setOnClickListener { confirmClear() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.records.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun render(records: List<ScanRecord>) {
        adapter.submitList(records)

        val empty = records.isEmpty()
        emptyHistory.visibility = if (empty) View.VISIBLE else View.GONE
        historyList.visibility = if (empty) View.GONE else View.VISIBLE
        exportButton.isEnabled = !empty
        clearButton.isEnabled = !empty

        val summary = viewModel.summary()
        summaryStats.text = getString(
            R.string.history_summary_format,
            summary.scans,
            summary.detected,
            summary.counted,
            summary.modelErrors,
            (summary.errorRate * 100).toInt(),
            (summary.averageConfidence * 100).toInt()
        )
    }

    private fun exportCsv() {
        val records = viewModel.records.value
        if (records.isEmpty()) return

        runCatching { CsvExporter.export(requireContext(), records) }
            .onSuccess { uri -> shareFile(uri) }
            .onFailure {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.history_export_failed)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
    }

    private fun shareFile(uri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.history_export_title))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.history_export_title)))
    }

    private fun confirmClear() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.history_clear_title)
            .setMessage(R.string.history_clear_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.clear() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDetails(record: ScanRecord) {
        val lines = record.entries.joinToString("\n") { entry ->
            val percent = (entry.confidence * 100).toInt()
            "${entry.partNum}  ${entry.partName}\n" +
                "   ${entry.colorName} · $percent% · ${entry.status}"
        }.ifBlank { getString(R.string.result_nothing) }

        val formatted = SimpleDateFormat(DETAIL_PATTERN, Locale.getDefault())
            .format(Date(record.timestamp))

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.history_details_title, formatted))
            .setMessage(lines)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private companion object {
        const val DETAIL_PATTERN = "dd.MM.yyyy HH:mm"
    }
}
