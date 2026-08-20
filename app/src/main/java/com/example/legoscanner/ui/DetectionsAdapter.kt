package com.example.legoscanner.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.legoscanner.Config
import com.example.legoscanner.R
import com.example.legoscanner.data.Detection
import com.example.legoscanner.data.DetectionStatus
import com.google.android.material.button.MaterialButton

class DetectionsAdapter(
    private val onConfirm: (Detection) -> Unit,
    private val onCorrect: (Detection) -> Unit
) : ListAdapter<Detection, DetectionsAdapter.DetectionViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detection, parent, false)
        return DetectionViewHolder(view, onConfirm, onCorrect)
    }

    override fun onBindViewHolder(holder: DetectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DetectionViewHolder(
        itemView: View,
        private val onConfirm: (Detection) -> Unit,
        private val onCorrect: (Detection) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.detectionImage)
        private val sampledColorDot: View = itemView.findViewById(R.id.sampledColorDot)
        private val name: TextView = itemView.findViewById(R.id.detectionName)
        private val meta: TextView = itemView.findViewById(R.id.detectionMeta)
        private val confidence: TextView = itemView.findViewById(R.id.detectionConfidence)
        private val bar: View = itemView.findViewById(R.id.confidenceBar)
        private val reviewButtons: LinearLayout = itemView.findViewById(R.id.reviewButtons)
        private val confirmButton: MaterialButton = itemView.findViewById(R.id.btnConfirm)
        private val correctButton: MaterialButton = itemView.findViewById(R.id.btnCorrect)

        fun bind(item: Detection) {
            val color = DetectionOverlayView.confidenceColor(item.confidence)
            val percent = (item.confidence * 100).toInt()

            name.text = item.matchedPart.name
            meta.text = itemView.context.getString(
                R.string.detection_full_meta,
                item.matchedPart.colorName,
                shapeLabel(item.confidence),
                statusLabel(item)
            )

            confidence.text = itemView.context.getString(R.string.percent_format, percent)
            confidence.setTextColor(color)

            bar.setBackgroundColor(color)
            bar.layoutParams = bar.layoutParams.apply {
                width = (itemView.resources.displayMetrics.density * percent).toInt()
            }
            bar.requestLayout()

            sampledColorDot.setBackgroundColor(item.sampledColor)

            image.load(item.matchedPart.imgUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }

            val needsReview = item.status == DetectionStatus.NEEDS_REVIEW
            reviewButtons.visibility = if (needsReview) View.VISIBLE else View.GONE
            itemView.alpha = if (item.status == DetectionStatus.DISMISSED) 0.4f else 1f

            confirmButton.setOnClickListener { onConfirm(item) }
            correctButton.setOnClickListener { onCorrect(item) }
        }

        private fun shapeLabel(confidence: Float): String = itemView.context.getString(
            when {
                confidence >= Config.CONFIDENCE_ACCEPT -> R.string.confidence_certain
                confidence >= Config.CONFIDENCE_REVIEW -> R.string.confidence_review
                else -> R.string.confidence_low
            }
        )

        private fun statusLabel(item: Detection): String {
            if (item.status == DetectionStatus.NEEDS_REVIEW && item.colorAmbiguous) {
                return itemView.context.getString(R.string.color_ambiguous)
            }
            return itemView.context.getString(
                when (item.status) {
                    DetectionStatus.AUTO_ACCEPTED -> R.string.status_auto_accepted
                    DetectionStatus.NEEDS_REVIEW -> R.string.status_needs_review
                    DetectionStatus.CONFIRMED -> R.string.status_confirmed
                    DetectionStatus.CORRECTED -> R.string.status_corrected
                    DetectionStatus.DISMISSED -> R.string.status_dismissed
                }
            )
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Detection>() {
            override fun areItemsTheSame(oldItem: Detection, newItem: Detection): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Detection, newItem: Detection): Boolean =
                oldItem == newItem
        }
    }
}
