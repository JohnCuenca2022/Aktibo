package com.example.aktibo

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface EdamamService {
    @GET("api/food-database/v2/parser")
    fun findFood(
        @Query("app_id") app_id: String,
        @Query("app_key") app_key: String,
        @Query("ingr") ingr: String,
        @Query("nutrition-type") nutritionType: String,
    ): Call<EdamamResponse>
}