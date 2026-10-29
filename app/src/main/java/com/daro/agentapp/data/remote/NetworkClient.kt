package com.daro.agentapp.data.remote

import com.daro.agentapp.core.config.EnvironmentConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

object NetworkClient {
    fun create(allowDevelopmentHttp: Boolean = false): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            if (!request.url.isHttps && !(allowDevelopmentHttp && request.url.host == EnvironmentConfig.DEVELOPMENT_HOST)) {
                throw IOException("Cleartext host is not allowed")
            }
            chain.proceed(request)
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
}
