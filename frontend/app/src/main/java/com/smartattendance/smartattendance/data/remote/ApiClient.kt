package com.smartattendance.smartattendance.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Physical device: PC's WiFi IP. For emulator, use 10.0.2.2
    private const val BASE_URL = "http://192.168.10.40:8000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
