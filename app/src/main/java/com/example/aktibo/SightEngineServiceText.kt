package com.example.aktibo

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface SightEngineServiceText {
    @POST("text/check.json")
    fun checkText(
        @Query("text") text: String,
        @Query("mode") mode: String,
        @Query("lang") lang: String,
        @Query("opt_countries") opt_countries: String,
        @Query("api_user") apiUser: String,
        @Query("api_secret") apiSecret: String
    ): Call<SightEngineResponseText>

}
