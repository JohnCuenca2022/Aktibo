package com.example.aktibo

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface SightEngineService {
    @Multipart
    @POST("check-workflow.json")
    fun checkWorkflow(
        @Part image: MultipartBody.Part,
        @Query("workflow") workflow: String,
        @Query("api_user") apiUser: String,
        @Query("api_secret") apiSecret: String
    ): Call<SightEngineResponse>
}