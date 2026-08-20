package com.example.legoscanner

object Config {
    const val DEFAULT_SET_NUM = "30510-1"
    const val CONFIDENCE_ACCEPT = 0.75f
    const val CONFIDENCE_REVIEW = 0.40f

    val rebrickableApiKey: String get() = BuildConfig.REBRICKABLE_API_KEY
    val roboflowApiKey: String get() = BuildConfig.ROBOFLOW_API_KEY
    val roboflowModelId: String get() = BuildConfig.ROBOFLOW_MODEL_ID
}
