package com.example.aktibo

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SightEngineClient {
    private const val BASE_URL = "https://api.sightengine.com/1.0/"

    private var retrofit: Retrofit? = null

    fun getClient(): Retrofit {
        if (retrofit == null) {
            val httpClient = OkHttpClient.Builder()
            httpClient.addInterceptor { chain ->
                val request: Request = chain.request()
                    .newBuilder()
                    .addHeader("Content-Type", "multipart/form-data")
                    .build()
                chain.proceed(request)
            }

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
}
