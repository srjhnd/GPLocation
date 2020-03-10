package com.srjhnd.gplocation.api

import com.srjhnd.gplocation.data.Location
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.PUT


interface GPLocationAPIService {
    @PUT("api/latestLocation/")
    suspend fun putLocation(@Body location: Location): ResponseBody
}