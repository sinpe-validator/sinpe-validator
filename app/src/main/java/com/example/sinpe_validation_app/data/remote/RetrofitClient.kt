package com.example.sinpe_validation_app.data.remote

import com.example.sinpe_validation_app.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val PORT = "5081"
    private val BASE_URL = "http://${BuildConfig.BACKEND_IP}:$PORT/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: SinpeBackendApi by lazy {
        retrofit.create(SinpeBackendApi::class.java)
    }
}
