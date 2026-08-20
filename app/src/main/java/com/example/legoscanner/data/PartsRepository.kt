package com.example.legoscanner.data

import retrofit2.HttpException
import java.io.IOException

class SetNotFoundException(val setNum: String) : Exception()
class InvalidApiKeyException : Exception()
class RateLimitException : Exception()
class NoNetworkException : Exception()

class PartsRepository(private val api: RebrickableApi = ApiClient.rebrickable) {

    suspend fun loadSetParts(setNum: String): List<PartRow> {
        val response = try {
            api.getSetParts(setNum)
        } catch (e: HttpException) {
            throw when (e.code()) {
                401, 403 -> InvalidApiKeyException()
                404 -> SetNotFoundException(setNum)
                429 -> RateLimitException()
                else -> e
            }
        } catch (e: IOException) {
            throw NoNetworkException()
        }

        return response.results
            .filterNot { it.isSpare }
            .map { it.toPartRow() }
            .sortedByDescending { it.required }
    }

    private fun InventoryPart.toPartRow() = PartRow(
        partNum = part.partNum,
        name = part.name,
        colorName = color.name,
        colorRgb = color.rgb,
        imgUrl = part.imgUrl,
        required = quantity
    )
}
