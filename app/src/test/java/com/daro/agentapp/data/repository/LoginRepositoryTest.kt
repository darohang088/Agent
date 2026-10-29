package com.daro.agentapp.data.repository

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.data.remote.RetrofitProvider
import com.daro.agentapp.data.remote.api.AuthApi
import com.daro.agentapp.domain.model.LoginRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LoginRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        client = OkHttpClient.Builder().retryOnConnectionFailure(false).followRedirects(false)
            .callTimeout(2, TimeUnit.SECONDS).build()
    }

    @After fun tearDown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        server.shutdown()
    }

    private fun repository(httpClient: OkHttpClient = client) = DefaultAuthRepository(
        RetrofitProvider.create(server.url("/").toString(), httpClient).create(AuthApi::class.java),
    )
    private fun request(password: String = " synthetic-password ") = LoginRequest("+15555550123", password)

    @Test fun `login posts exactly two fields and maps the complete session`() = runBlocking {
        server.enqueue(MockResponse().setBody(successBody))
        val result = repository().login(request())
        assertTrue(result is AppResult.Success)
        val session = (result as AppResult.Success).value
        assertEquals("synthetic-access", session.accessToken)
        assertEquals("synthetic-refresh", session.refreshToken)
        assertEquals("Bearer", session.tokenType)
        assertEquals(3600L, session.expiresIn)
        assertEquals(7L, session.user.id)
        assertEquals("+15555550123", session.user.phoneNumber)
        assertEquals("PHONE", session.user.authProvider)
        assertEquals("USER", session.user.role)
        val sent = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("POST", sent.method)
        assertEquals("/api/v1/auth/login", sent.path)
        assertEquals("application/json", sent.getHeader("Accept"))
        assertTrue(sent.getHeader("Content-Type")!!.startsWith("application/json"))
        assertNull(sent.getHeader("Authorization"))
        val body = Json.parseToJsonElement(sent.body.readUtf8()).jsonObject
        assertEquals(setOf("phoneNumber", "password"), body.keys)
        assertEquals("+15555550123", body.getValue("phoneNumber").jsonPrimitive.content)
        assertEquals(" synthetic-password ", body.getValue("password").jsonPrimitive.content)
        assertFalse(request().toString().contains("synthetic-password"))
        assertFalse(session.toString().contains("synthetic-access"))
    }

    @Test fun `login http errors preserve status without retaining private server messages`() = runBlocking {
        for (status in listOf(400, 401, 403, 429, 500)) {
            server.enqueue(MockResponse().setResponseCode(status).setBody("private server details"))
            assertEquals(AppResult.Failure(AppError.Http(status)), repository().login(request()))
        }
        assertEquals(5, server.requestCount)
    }

    @Test fun `login business failure maps to rejection`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":false,"message":"private message"}"""))
        assertEquals(AppResult.Failure(AppError.Rejected), repository().login(request()))
    }

    @Test fun `login rejects malformed missing and invalid session fields`() = runBlocking {
        for (body in listOf("not json", "{}", "{\"success\":true}",
            successBody.replace("synthetic-access", ""), successBody.replace("3600", "-1"))) {
            server.enqueue(MockResponse().setBody(body))
            assertEquals(AppResult.Failure(AppError.InvalidResponse), repository().login(request()))
        }
    }

    @Test fun `empty credentials make no request`() = runBlocking {
        for (request in listOf(LoginRequest("", "password"), LoginRequest("   ", "password"), request(""))) {
            assertEquals(AppResult.Failure(AppError.InvalidInput), repository().login(request))
        }
        assertEquals(0, server.requestCount)
    }

    @Test fun `login accepts the zero expiry shown by the api without inventing a lifetime`() = runBlocking {
        server.enqueue(MockResponse().setBody(successBody.replace("3600", "0")))
        val result = repository().login(request()) as AppResult.Success
        assertEquals(0L, result.value.expiresIn)
    }

    @Test fun `login does not automatically replay a service unavailable response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "0"))
        server.enqueue(MockResponse().setBody(successBody))
        assertEquals(AppResult.Failure(AppError.Http(503)), repository().login(request()))
        assertEquals(1, server.requestCount)
    }

    @Test fun `login timeout maps to typed failure`() = runBlocking {
        server.enqueue(MockResponse().setBody(successBody).setBodyDelay(1, TimeUnit.SECONDS))
        val shortClient = client.newBuilder().readTimeout(100, TimeUnit.MILLISECONDS).build()
        assertEquals(AppResult.Failure(AppError.Timeout), repository(shortClient).login(request()))
    }

    @Test fun `canceling login propagates cancellation`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val operation = async(Dispatchers.Default) { repository().login(request()) }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        operation.cancelAndJoin()
        assertTrue(operation.isCancelled)
    }

    @Test fun `unconfigured login fails without a transport request`() = runBlocking {
        assertEquals(AppResult.Failure(AppError.Configuration), UnconfiguredAuthRepository().login(request()))
        assertEquals(0, server.requestCount)
    }

    private val successBody = """
        {"success":true,"message":"Logged in","data":{
        "accessToken":"synthetic-access","refreshToken":"synthetic-refresh","tokenType":"Bearer",
        "expiresIn":3600,"user":{"id":7,"phoneNumber":"+15555550123","authProvider":"PHONE","role":"USER"}},
        "timestamp":"2026-10-29T09:11:56.899Z","futureField":"ignored"}
    """.trimIndent()
}
