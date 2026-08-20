package com.example.legoscanner.data

import android.graphics.Bitmap
import com.example.legoscanner.Config
import com.example.legoscanner.util.ColorMatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

class RoboflowNotConfiguredException : Exception()
class DetectionFailedException(val code: Int) : Exception()

class DetectionRepository(private val api: RoboflowApi = ApiClient.roboflow) {

    suspend fun detect(
        imageBase64: String,
        photo: Bitmap,
        setParts: List<PartRow>
    ): ScanResult {
        val apiKey = Config.roboflowApiKey
        val modelId = Config.roboflowModelId

        if (apiKey.isBlank() || modelId.isBlank()) throw RoboflowNotConfiguredException()

        val url = "https://serverless.roboflow.com/$modelId?api_key=$apiKey"
        val body = imageBase64.toRequestBody(FORM_URLENCODED)

        val response = try {
            api.detect(url, body)
        } catch (e: HttpException) {
            throw DetectionFailedException(e.code())
        } catch (_: IOException) {
            throw NoNetworkException()
        }

        val sourceWidth = response.image?.width ?: photo.width
        val sourceHeight = response.image?.height ?: photo.height
        val toBitmapX = photo.width.toFloat() / sourceWidth
        val toBitmapY = photo.height.toFloat() / sourceHeight

        val partsByNumber = setParts.groupBy { it.partNum }
        val detections = mutableListOf<Detection>()
        val rejected = mutableListOf<RejectedDetection>()

        response.predictions.forEachIndexed { index, prediction ->
            val partNum = ClassMapping.toPartNum(prediction.className)
            val candidates = partsByNumber[partNum].orEmpty()

            if (prediction.confidence < MIN_CONFIDENCE) {
                rejected += RejectedDetection(
                    prediction.className,
                    prediction.confidence,
                    RejectionReason.BELOW_THRESHOLD
                )
                return@forEachIndexed
            }

            if (candidates.isEmpty()) {
                rejected += RejectedDetection(
                    prediction.className,
                    prediction.confidence,
                    RejectionReason.NOT_IN_SET
                )
                return@forEachIndexed
            }

            val box = BoundingBox(
                prediction.x,
                prediction.y,
                prediction.width,
                prediction.height
            )

            val sampledColor = ColorMatcher.dominantColor(
                photo,
                box.left * toBitmapX,
                box.top * toBitmapY,
                box.width * toBitmapX,
                box.height * toBitmapY
            )

            val match = ColorMatcher.nearest(candidates, sampledColor) { it.colorRgb }
                ?: return@forEachIndexed

            val certainShape = prediction.confidence >= Config.CONFIDENCE_ACCEPT
            val certainColor = !match.ambiguous

            detections += Detection(
                id = index,
                partNum = partNum,
                className = prediction.className,
                confidence = prediction.confidence,
                box = box,
                matchedPart = match.value,
                sampledColor = sampledColor,
                colorAmbiguous = match.ambiguous,
                colorCandidates = candidates.size,
                status = if (certainShape && certainColor) {
                    DetectionStatus.AUTO_ACCEPTED
                } else {
                    DetectionStatus.NEEDS_REVIEW
                }
            )
        }

        return ScanResult(
            detections = detections.sortedByDescending { it.confidence },
            rejected = rejected,
            imageWidth = sourceWidth,
            imageHeight = sourceHeight
        )
    }

    private companion object {
        const val MIN_CONFIDENCE = 0.30f
        val FORM_URLENCODED = "application/x-www-form-urlencoded".toMediaType()
    }
}
