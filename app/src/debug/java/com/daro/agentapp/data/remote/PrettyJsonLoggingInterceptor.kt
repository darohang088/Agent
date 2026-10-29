package com.daro.agentapp.data.remote

import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

/** Source exists in debug only, shared by staging/uat/preProd/prod debug variants. */
internal class PrettyJsonLoggingInterceptor(private val sink: (String) -> Unit) : Interceptor {
    private val formatter = NetworkLogFormatter()
    private val outputLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBody = preview(request.body)
        val started = System.nanoTime()
        try {
            val response = chain.proceed(request)
            val body = try { response.peekBody(16_385).string() } catch (_: IOException) { null }
            emit(formatter.format(request, requestBody, response, body, elapsed(started)))
            return response
        } catch (e: IOException) {
            val category = when (e) {
                is SSLException -> "SecureConnection"
                is InterruptedIOException -> "Timeout"
                else -> "Connection or canceled"
            }
            emit(formatter.format(request, requestBody, null, null, elapsed(started), category))
            throw e
        }
    }

    private fun preview(body: RequestBody?): String? = try {
        when {
            body == null || body.isOneShot() || body.isDuplex() -> null
            body.contentType()?.subtype?.contains("json") != true -> null
            body.contentLength() !in 0..16_384 -> null
            else -> Buffer().also { body.writeTo(it) }.readUtf8()
        }
    } catch (_: Exception) { null }

    private fun emit(message: String) {
        // All values have already been sanitized. Sink failures must not affect a request.
        try {
            synchronized(outputLock) {
                message.lineSequence().forEach { line ->
                    line.chunked(900).ifEmpty { listOf("") }.forEach(sink)
                }
            }
        } catch (_: Exception) { }
    }

    private fun elapsed(started: Long) = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
}
