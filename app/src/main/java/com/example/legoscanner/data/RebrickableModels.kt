package com.example.legoscanner.data

import com.google.gson.annotations.SerializedName

data class SetPartsResponse(
    val count: Int,
    val next: String?,
    val results: List<InventoryPart>
)

data class InventoryPart(
    val part: Part,
    val color: PartColor,
    val quantity: Int,
    @SerializedName("is_spare") val isSpare: Boolean,
    @SerializedName("element_id") val elementId: String?
)

data class Part(
    @SerializedName("part_num") val partNum: String,
    val name: String,
    @SerializedName("part_img_url") val imgUrl: String?
)

data class PartColor(
    val id: Int,
    val name: String,
    val rgb: String?
)

data class PartRow(
    val partNum: String,
    val name: String,
    val colorId: Int,
    val colorName: String,
    val colorRgb: String?,
    val imgUrl: String?,
    val required: Int,
    val found: Int = 0
) {
    val key: String get() = "$partNum|$colorId"
    val isComplete: Boolean get() = found >= required
}
