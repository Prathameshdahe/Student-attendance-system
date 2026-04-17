package com.smartattendance.smartattendance.data.remote

import com.smartattendance.smartattendance.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val httpClient = OkHttpClient.Builder()
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                if (hostname == "fabulous-gratitude-production-9d95.up.railway.app") {
                    return listOf(java.net.InetAddress.getByName("151.101.2.15"))
                }
                return okhttp3.Dns.SYSTEM.lookup(hostname)
            }
        })
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("X-KIWI-Client-Type", "android-app")
                .header("X-KIWI-Device-ID", DeviceIdentity.getDeviceId())
            
            TokenManager.getToken()?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }
            
            chain.proceed(requestBuilder.build())
        }
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
