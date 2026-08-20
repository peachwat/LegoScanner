package com.example.legoscanner.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RebrickableApi {

    @GET("lego/sets/{setNum}/parts/")
    suspend fun getSetParts(
        @Path("setNum") setNum: String,
        @Query("page_size") pageSize: Int = 1000,
        @Query("inc_part_details") includeDetails: Int = 1
    ): SetPartsResponse
}
