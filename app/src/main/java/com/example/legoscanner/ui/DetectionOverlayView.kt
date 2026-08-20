package com.example.legoscanner.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.legoscanner.Config
import com.example.legoscanner.data.Detection
import kotlin.math.min

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var detections: List<Detection> = emptyList()
    private var sourceWidth = 0
    private var sourceHeight = 0

    private val boxRect = RectF()
    private val labelRect = RectF()

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 9f
        color = Color.parseColor("#CC000000")
    }

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val labelBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        isFakeBoldText = true
    }

    fun setDetections(items: List<Detection>, imageWidth: Int, imageHeight: Int) {
        detections = items
        sourceWidth = imageWidth
        sourceHeight = imageHeight
        invalidate()
    }

    fun clear() {
        detections = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (detections.isEmpty() || sourceWidth == 0 || sourceHeight == 0) return

        val scale = min(width.toFloat() / sourceWidth, height.toFloat() / sourceHeight)
        val offsetX = (width - sourceWidth * scale) / 2f
        val offsetY = (height - sourceHeight * scale) / 2f

        detections.forEach { detection ->
            val color = confidenceColor(detection.confidence)

            boxRect.set(
                offsetX + detection.box.left * scale,
                offsetY + detection.box.top * scale,
                offsetX + (detection.box.left + detection.box.width) * scale,
                offsetY + (detection.box.top + detection.box.height) * scale
            )

            canvas.drawRoundRect(boxRect, 6f, 6f, outlinePaint)
            boxPaint.color = color
            canvas.drawRoundRect(boxRect, 6f, 6f, boxPaint)

            val label = "${detection.partNum}  ${(detection.confidence * 100).toInt()}%"
            val textWidth = labelText.measureText(label)
            val labelHeight = 40f

            val labelTop = if (boxRect.top - labelHeight >= 0f) {
                boxRect.top - labelHeight
            } else {
                boxRect.bottom
            }
            val labelLeft = boxRect.left.coerceAtMost(width - textWidth - 24f).coerceAtLeast(0f)

            labelRect.set(labelLeft, labelTop, labelLeft + textWidth + 20f, labelTop + labelHeight)
            labelBackground.color = color
            canvas.drawRoundRect(labelRect, 4f, 4f, labelBackground)
            canvas.drawText(label, labelLeft + 10f, labelTop + 29f, labelText)
        }
    }

    companion object {
        fun confidenceColor(confidence: Float): Int = when {
            confidence >= Config.CONFIDENCE_ACCEPT -> Color.parseColor("#2E7D32")
            confidence >= Config.CONFIDENCE_REVIEW -> Color.parseColor("#EF6C00")
            else -> Color.parseColor("#C62828")
        }
    }
}
