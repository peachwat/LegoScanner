package com.example.legoscanner

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.legoscanner.data.PartRow

class PartsAdapter(
    private val onAdjust: (PartRow, Int) -> Unit
) : ListAdapter<PartRow, PartsAdapter.PartViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_part, parent, false)
        return PartViewHolder(view, onAdjust)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PartViewHolder(
        itemView: View,
        private val onAdjust: (PartRow, Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.partImage)
        private val colorDot: View = itemView.findViewById(R.id.colorDot)
        private val name: TextView = itemView.findViewById(R.id.partName)
        private val meta: TextView = itemView.findViewById(R.id.partMeta)
        private val counter: TextView = itemView.findViewById(R.id.partCounter)
        private val minusButton: View = itemView.findViewById(R.id.partMinus)
        private val plusButton: View = itemView.findViewById(R.id.partPlus)

        fun bind(item: PartRow) {
            name.text = item.name
            meta.text = itemView.context.getString(
                R.string.part_meta_format, item.partNum, item.colorName
            )
            counter.text = itemView.context.getString(
                R.string.part_counter_format, item.found, item.required
            )
            counter.setTextColor(if (item.isComplete) COLOR_COMPLETE else COLOR_PENDING)
            colorDot.setBackgroundColor(parseRgb(item.colorRgb))

            minusButton.setOnClickListener { onAdjust(item, -1) }
            plusButton.setOnClickListener { onAdjust(item, 1) }

            image.load(item.imgUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
                crossfade(true)
            }
        }

        private fun parseRgb(rgb: String?): Int {
            if (rgb.isNullOrBlank()) return Color.LTGRAY
            return runCatching { Color.parseColor("#$rgb") }.getOrDefault(Color.LTGRAY)
        }
    }

    private companion object {
        val COLOR_COMPLETE = Color.parseColor("#2E7D32")
        val COLOR_PENDING = Color.parseColor("#757575")

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PartRow>() {
            override fun areItemsTheSame(oldItem: PartRow, newItem: PartRow): Boolean =
                oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: PartRow, newItem: PartRow): Boolean =
                oldItem == newItem
        }
    }
}
