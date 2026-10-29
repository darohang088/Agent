package com.daro.agentapp.data.remote

import java.util.UUID
import okhttp3.Interceptor
import okhttp3.Response

/** Common public headers only. Authentication belongs in a separate session-aware interceptor. */
internal class RequestHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-Correlation-ID", UUID.randomUUID().toString())
            .build(),
    )
}
