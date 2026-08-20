package com.example.legoscanner.data

import com.google.gson.annotations.SerializedName

data class DetectionResponse(
    val predictions: List<Prediction> = emptyList(),
    val image: ImageSize?
)

data class Prediction(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Float,
    @SerializedName("class") val className: String
)

data class ImageSize(
    val width: Int,
    val height: Int
)

data class BoundingBox(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float
) {
    val left: Float get() = centerX - width / 2
    val top: Float get() = centerY - height / 2
}

data class Detection(
    val id: Int,
    val partNum: String,
    val className: String,
    val confidence: Float,
    val box: BoundingBox,
    val matchedPart: PartRow,
    val sampledColor: Int,
    val colorAmbiguous: Boolean,
    val colorCandidates: Int,
    val status: DetectionStatus
)

enum class DetectionStatus {
    AUTO_ACCEPTED,
    NEEDS_REVIEW,
    CONFIRMED,
    CORRECTED,
    DISMISSED
}

data class RejectedDetection(
    val className: String,
    val confidence: Float,
    val reason: RejectionReason
)

enum class RejectionReason {
    BELOW_THRESHOLD,
    NOT_IN_SET
}

data class ScanResult(
    val detections: List<Detection>,
    val rejected: List<RejectedDetection>,
    val imageWidth: Int,
    val imageHeight: Int
) {
    val accepted: List<Detection>
        get() = detections.filter { it.status != DetectionStatus.DISMISSED }

    val needingReview: List<Detection>
        get() = detections.filter { it.status == DetectionStatus.NEEDS_REVIEW }
}
