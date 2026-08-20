package com.example.legoscanner.data

data class ScanRecord(
    val id: Long,
    val setNum: String,
    val timestamp: Long,
    val entries: List<ScanEntry>,
    val rejectedNotInSet: Int,
    val rejectedLowConfidence: Int
) {
    val detected: Int get() = entries.size

    val autoAccepted: Int get() = entries.count { it.status == DetectionStatus.AUTO_ACCEPTED.name }
    val confirmed: Int get() = entries.count { it.status == DetectionStatus.CONFIRMED.name }
    val corrected: Int get() = entries.count { it.status == DetectionStatus.CORRECTED.name }
    val dismissed: Int get() = entries.count { it.status == DetectionStatus.DISMISSED.name }
    val pending: Int get() = entries.count { it.status == DetectionStatus.NEEDS_REVIEW.name }

    val counted: Int get() = autoAccepted + confirmed + corrected

    val averageConfidence: Float
        get() = if (entries.isEmpty()) 0f else entries.map { it.confidence }.average().toFloat()

    /** Ile razy model się pomylił — użytkownik musiał poprawić lub odrzucić wynik. */
    val modelErrors: Int get() = corrected + dismissed
}

data class ScanEntry(
    val partNum: String,
    val partName: String,
    val colorName: String,
    val confidence: Float,
    val colorAmbiguous: Boolean,
    val status: String,
    val correctedToPartNum: String? = null,
    val correctedToColorName: String? = null
)
