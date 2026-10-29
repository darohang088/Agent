package com.daro.agentapp.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Create once in AppContainer; feature services share configuration and connection pools. */
internal object RetrofitProvider {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(
        baseUrl: String,
        client: OkHttpClient,
        loggingInterceptors: List<Interceptor> = emptyList(),
    ): Retrofit {
        val configuredClient = client.newBuilder()
            .addInterceptor(RequestHeadersInterceptor())
            .apply { loggingInterceptors.forEach { addInterceptor(it) } }
            .addInterceptor(NonReplayInterceptor())
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(configuredClient)
            .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .build()
    }
}
