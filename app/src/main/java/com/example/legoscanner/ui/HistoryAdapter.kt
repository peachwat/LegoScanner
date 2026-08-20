package com.example.legoscanner.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.legoscanner.R
import com.example.legoscanner.data.ScanRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onClick: (ScanRecord) -> Unit
) : ListAdapter<ScanRecord, HistoryAdapter.RecordViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_record, parent, false)
        return RecordViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecordViewHolder(
        itemView: View,
        private val onClick: (ScanRecord) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.recordTitle)
        private val time: TextView = itemView.findViewById(R.id.recordTime)
        private val summary: TextView = itemView.findViewById(R.id.recordSummary)
        private val errors: TextView = itemView.findViewById(R.id.recordErrors)

        fun bind(record: ScanRecord) {
            val context = itemView.context

            title.text = context.getString(R.string.set_title_format, record.setNum)
            time.text = SimpleDateFormat(TIME_PATTERN, Locale.getDefault())
                .format(Date(record.timestamp))

            summary.text = context.getString(
                R.string.history_record_summary,
                record.detected,
                record.counted,
                record.pending,
                (record.averageConfidence * 100).toInt()
            )

            errors.text = context.getString(
                R.string.history_record_errors,
                record.modelErrors,
                record.rejectedNotInSet
            )
            errors.setTextColor(
                if (record.modelErrors > 0) Color.parseColor("#C62828")
                else Color.parseColor("#2E7D32")
            )

            itemView.setOnClickListener { onClick(record) }
        }

        private companion object {
            const val TIME_PATTERN = "dd.MM HH:mm"
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ScanRecord>() {
            override fun areItemsTheSame(oldItem: ScanRecord, newItem: ScanRecord): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ScanRecord, newItem: ScanRecord): Boolean =
                oldItem == newItem
        }
    }
}
