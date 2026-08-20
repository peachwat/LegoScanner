package com.example.legoscanner.data

object ClassMapping {

    private val classToPartNum = mapOf(
        "2x4_brick" to "3001",
        "2x2_brick" to "3003",
        "1x2_brick" to "3004",
        "1x1_brick" to "3005",
        "1x4_brick" to "3010",
        "2x4_plate" to "3020",
        "2x2_plate" to "3022",
        "1x2_plate" to "3023",
        "1x1_plate" to "3024",
        "1x4_plate" to "3710",
        "brick_2x4" to "3001",
        "brick_2x2" to "3003",
        "brick_1x2" to "3004",
        "brick_1x1" to "3005",
        "plate_2x4" to "3020",
        "plate_2x2" to "3022",
        "plate_1x2" to "3023",
        "plate_1x1" to "3024",
        "tile_2x2" to "3068b",
        "tile_1x2" to "3069b",
        "wheel" to "34337",
        "tyre" to "87414",
        "tire" to "87414"
    )

    fun toPartNum(className: String): String =
        classToPartNum[className.lowercase().replace(' ', '_')] ?: className
}
