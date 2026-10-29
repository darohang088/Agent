package com.daro.agentapp.data.remote

import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

/** Mutating requests must not be replayed by HTTP follow-ups (e.g. 503 Retry-After: 0). */
internal class NonReplayInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body
        if (body == null || request.method !in setOf("POST", "PUT", "PATCH", "DELETE")) {
            return chain.proceed(request)
        }
        val oneShot = object : RequestBody() {
            override fun contentType() = body.contentType()
            override fun contentLength() = body.contentLength()
            override fun isDuplex() = body.isDuplex()
            override fun isOneShot() = true
            override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
        }
        return chain.proceed(request.newBuilder().method(request.method, oneShot).build())
    }
}
