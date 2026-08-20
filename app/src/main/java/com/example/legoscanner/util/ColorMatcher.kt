package com.example.legoscanner.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

data class ColorMatch<T>(
    val value: T,
    val distance: Double,
    val ambiguous: Boolean
)

object ColorMatcher {

    private const val SAMPLE_GRID = 14

    /** Ile procent szerokości ramki bierzemy pod uwagę. Mniej = mniej tła w próbce. */
    private const val INNER_RATIO = 0.45f

    /** Jeśli dwa najbliższe kolory różnią się mniej niż o tę wartość, wynik jest niepewny. */
    private const val AMBIGUITY_MARGIN = 60.0

    /** Powyżej tej odległości dopasowanie uznajemy za niewiarygodne. */
    private const val MAX_TRUSTED_DISTANCE = 110.0

    fun dominantColor(bitmap: Bitmap, left: Float, top: Float, width: Float, height: Float): Int {
        val insetX = width * (1 - INNER_RATIO) / 2
        val insetY = height * (1 - INNER_RATIO) / 2

        val startX = (left + insetX).toInt().coerceIn(0, bitmap.width - 1)
        val startY = (top + insetY).toInt().coerceIn(0, bitmap.height - 1)
        val endX = (left + width - insetX).toInt().coerceIn(startX + 1, bitmap.width)
        val endY = (top + height - insetY).toInt().coerceIn(startY + 1, bitmap.height)

        val reds = ArrayList<Int>(SAMPLE_GRID * SAMPLE_GRID)
        val greens = ArrayList<Int>(SAMPLE_GRID * SAMPLE_GRID)
        val blues = ArrayList<Int>(SAMPLE_GRID * SAMPLE_GRID)

        val stepX = ((endX - startX) / SAMPLE_GRID).coerceAtLeast(1)
        val stepY = ((endY - startY) / SAMPLE_GRID).coerceAtLeast(1)

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val pixel = bitmap.getPixel(x, y)
                reds += Color.red(pixel)
                greens += Color.green(pixel)
                blues += Color.blue(pixel)
                x += stepX
            }
            y += stepY
        }

        if (reds.isEmpty()) return Color.GRAY

        return Color.rgb(median(reds), median(greens), median(blues))
    }

    fun <T> nearest(candidates: List<T>, sampled: Int, rgbOf: (T) -> String?): ColorMatch<T>? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return ColorMatch(candidates.first(), 0.0, false)

        val scored = candidates
            .mapNotNull { candidate ->
                val reference = parseHex(rgbOf(candidate)) ?: return@mapNotNull null
                candidate to distance(sampled, reference)
            }
            .sortedBy { it.second }

        if (scored.isEmpty()) return ColorMatch(candidates.first(), Double.MAX_VALUE, true)

        val best = scored.first()
        val tooClose = scored.size > 1 && (scored[1].second - best.second) < AMBIGUITY_MARGIN
        val tooFar = best.second > MAX_TRUSTED_DISTANCE

        return ColorMatch(best.first, best.second, tooClose || tooFar)
    }

    /**
     * Odległość w przestrzeni RGB z korektą percepcyjną (tzw. redmean).
     * Zwykła odległość euklidesowa źle oddaje różnice widziane okiem,
     * ta formuła waży składowe w zależności od jasności czerwieni.
     */
    private fun distance(a: Int, b: Int): Double {
        val rMean = (Color.red(a) + Color.red(b)) / 2.0
        val dr = (Color.red(a) - Color.red(b)).toDouble()
        val dg = (Color.green(a) - Color.green(b)).toDouble()
        val db = (Color.blue(a) - Color.blue(b)).toDouble()

        return sqrt(
            (2 + rMean / 256) * dr * dr +
                4 * dg * dg +
                (2 + (255 - rMean) / 256) * db * db
        )
    }

    private fun parseHex(rgb: String?): Int? {
        if (rgb.isNullOrBlank()) return null
        return runCatching { Color.parseColor("#$rgb") }.getOrNull()
    }

    private fun median(values: MutableList<Int>): Int {
        values.sort()
        return values[values.size / 2]
    }
}
