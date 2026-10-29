package com.daro.agentapp.data.remote

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.data.remote.api.AuthApi
import com.daro.agentapp.data.repository.DefaultAuthRepository
import com.daro.agentapp.domain.model.RegisterRequest
import com.daro.agentapp.domain.model.LoginRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class PrettyJsonLoggingInterceptorTest {
    @Test
    fun `every debug flavor installs a pretty logging interceptor`() {
        assertTrue(NetworkLogging.interceptors().single() is PrettyJsonLoggingInterceptor)
    }

    @Test
    fun `grouped logs include service headers pretty json and redact private values`() = runBlocking {
        withServer { server, lines, repository ->
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
                .setHeader("Set-Cookie", "private-cookie")
                .setBody("""{"success":false,"message":"private-message","extra":"unknown-secret"}"""))
            assertEquals(AppResult.Failure(AppError.Rejected), repository.register(request()))
            val output = lines.joinToString("\n")
            assertTrue(output.contains("service = AuthApi"))
            assertTrue(output.contains("method = POST register"))
            assertTrue(output.contains("status = 200"))
            assertTrue(output.contains("request headers ="))
            assertTrue(output.contains("response headers ="))
            assertTrue(output.contains("    \"success\": false"))
            assertTrue(output.contains("\"password\": \"[REDACTED]\""))
            for (secret in listOf("private-cookie", "private-message", "unknown-secret",
                "private-password", "private-name", "private-location", "+15555550123",
                "private-auth", "private-request-cookie")) {
                assertFalse("Leaked fixture field", output.contains(secret))
            }
        }
    }

    @Test
    fun `login logs the endpoint and hides credentials and server messages`() = runBlocking {
        withServer { server, lines, repository ->
            server.enqueue(MockResponse().setBody("""{"success":false,"message":"private-login-message"}"""))
            assertEquals(AppResult.Failure(AppError.Rejected), repository.login(
                LoginRequest("+15555550123", "private-login-password"),
            ))
            val output = lines.joinToString("\n")
            assertTrue(output.contains("method = POST login"))
            assertTrue(output.contains("endpoint = api/v1/auth/login"))
            assertFalse(output.contains("private-login-password"))
            assertFalse(output.contains("private-login-message"))
            assertFalse(output.contains("+15555550123"))
        }
    }

    @Test
    fun `response remains parsable and credentials are redacted`() = runBlocking {
        withServer { server, lines, repository ->
            server.enqueue(MockResponse().setBody("""{"success":true,"data":{
                "accessToken":"private-access","refreshToken":"private-refresh","tokenType":"Bearer",
                "expiresIn":3600,"user":{"id":7,"phoneNumber":"+15555550123",
                "authProvider":"PHONE","role":"USER"}}}"""))
            assertTrue(repository.register(request()) is AppResult.Success)
            val output = lines.joinToString("\n")
            assertFalse(output.contains("private-access"))
            assertFalse(output.contains("private-refresh"))
            assertTrue(output.contains("\"expiresIn\": 3600"))
        }
    }

    @Test
    fun `invalid large and http error bodies have no raw fallback`() = runBlocking {
        withServer { server, lines, repository ->
            server.enqueue(MockResponse().setResponseCode(400).setBody("private-non-json"))
            assertEquals(AppResult.Failure(AppError.Http(400)), repository.register(request()))
            server.enqueue(MockResponse().setResponseCode(500).setBody("private-large".repeat(2000)))
            assertEquals(AppResult.Failure(AppError.Http(500)), repository.register(request()))
            val output = lines.joinToString("\n")
            assertFalse(output.contains("private-non-json"))
            assertFalse(output.contains("private-large"))
            assertTrue(output.contains("omitted"))
        }
    }

    @Test
    fun `broken log sink cannot break registration`() = runBlocking {
        val server = MockWebServer()
        val client = OkHttpClient.Builder().addInterceptor(
            PrettyJsonLoggingInterceptor { throw IllegalStateException("sink failed") },
        ).build()
        try {
            server.start()
            server.enqueue(MockResponse().setBody("""{"success":false}"""))
            val api = RetrofitProvider.create(server.url("/").toString(), client).create(AuthApi::class.java)
            assertEquals(AppResult.Failure(AppError.Rejected), DefaultAuthRepository(api).register(request()))
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    private suspend fun withServer(block: suspend (MockWebServer, List<String>, DefaultAuthRepository) -> Unit) {
        val server = MockWebServer()
        val lines = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder()
                .header("Authorization", "private-auth")
                .header("Cookie", "private-request-cookie").build()) }
            .addInterceptor(PrettyJsonLoggingInterceptor(lines::add))
            .callTimeout(2, TimeUnit.SECONDS).build()
        try {
            server.start()
            val api = RetrofitProvider.create(server.url("/").toString(), client).create(AuthApi::class.java)
            block(server, lines, DefaultAuthRepository(api))
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    private fun request() = RegisterRequest(
        "+15555550123", "private-password", "private-name", "private-name", "private-name",
        "2000-01-01", "MALE", 12.0, 13.0, "private-location", listOf("private-interest"),
    )
}
