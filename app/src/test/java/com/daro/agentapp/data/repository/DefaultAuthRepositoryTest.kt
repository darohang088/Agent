package com.daro.agentapp.data.repository

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.data.remote.NetworkClient
import com.daro.agentapp.data.remote.RetrofitProvider
import com.daro.agentapp.data.remote.api.AuthApi
import com.daro.agentapp.domain.model.RegisterRequest
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

class DefaultAuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .callTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        server.shutdown()
    }

    private fun repository(httpClient: OkHttpClient = client) =
        DefaultAuthRepository(RetrofitProvider.create(server.url("/").toString(), httpClient).create(AuthApi::class.java))

    private fun request(latitude: Double = -90.0) = RegisterRequest(
        phoneNumber = "+15555550123", password = "synthetic-password",
        firstName = "Test", lastName = "User", displayName = "Test User",
        dateBirth = "2000-01-01", gender = "MALE", latitude = latitude,
        longitude = -180.0, locationName = "Test location", interested = listOf("music"),
    )

    @Test
    fun `posts contract fields and maps session`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody(successBody))
        val result = repository().register(request())
        assertTrue(result is AppResult.Success)
        val session = (result as AppResult.Success).value
        assertEquals("test-access", session.accessToken)
        assertEquals("test-refresh", session.refreshToken)
        assertEquals("Bearer", session.tokenType)
        assertEquals(3600L, session.expiresIn)
        assertEquals(42L, session.user.id)
        assertEquals("PHONE", session.user.authProvider)
        assertEquals("USER", session.user.role)
        val sent = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("POST", sent.method)
        assertEquals("/api/v1/auth/register", sent.path)
        assertTrue(sent.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals("application/json", sent.getHeader("Accept"))
        assertNull(sent.getHeader("Authorization"))
        val body = Json.parseToJsonElement(sent.body.readUtf8()).jsonObject
        assertEquals(setOf("phoneNumber", "password", "firstName", "lastName", "displayName",
            "dateBirth", "gender", "latitude", "longitude", "locationName", "interested"), body.keys)
        assertEquals("synthetic-password", body.getValue("password").jsonPrimitive.content)
        assertEquals("2000-01-01", body.getValue("dateBirth").jsonPrimitive.content)
        assertFalse(session.toString().contains("test-access"))
        assertFalse(request().toString().contains("synthetic-password"))
    }

    @Test
    fun `http errors retain status without leaking raw body`() = runBlocking {
        for (status in listOf(400, 401, 409, 422, 429, 500)) {
            server.enqueue(MockResponse().setResponseCode(status).setBody("sensitive server body"))
            assertEquals(AppResult.Failure(AppError.Http(status)), repository().register(request()))
        }
        assertEquals(6, server.requestCount)
    }

    @Test
    fun `service unavailable does not replay registration`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "0"))
        server.enqueue(MockResponse().setBody(successBody))
        assertEquals(AppResult.Failure(AppError.Http(503)), repository().register(request()))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `empty successful response is invalid`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        assertEquals(AppResult.Failure(AppError.InvalidResponse), repository().register(request()))
    }

    @Test
    fun `business rejection is typed`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":false,"message":"private details"}"""))
        assertEquals(AppResult.Failure(AppError.Rejected), repository().register(request()))
    }

    @Test
    fun `invalid and incomplete responses are rejected`() = runBlocking {
        for (body in listOf("not json", "{}", "{\"success\":true}",
            successBody.replace("test-access", ""), successBody.replace("3600", "-1"))) {
            server.enqueue(MockResponse().setBody(body))
            assertEquals(AppResult.Failure(AppError.InvalidResponse), repository().register(request()))
        }
    }

    @Test
    fun `invalid coordinates do not make a request`() = runBlocking {
        for (latitude in listOf(Double.NaN, Double.POSITIVE_INFINITY, 91.0)) {
            assertEquals(AppResult.Failure(AppError.InvalidInput), repository().register(request(latitude)))
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `slow response produces timeout`() = runBlocking {
        server.enqueue(MockResponse().setBody(successBody).setBodyDelay(1, TimeUnit.SECONDS))
        val shortClient = client.newBuilder().readTimeout(100, TimeUnit.MILLISECONDS).build()
        assertEquals(AppResult.Failure(AppError.Timeout), repository(shortClient).register(request()))
    }

    @Test
    fun `unreachable endpoint produces connection error`() = runBlocking {
        val url = server.url("/").toString()
        server.shutdown()
        val repo = DefaultAuthRepository(RetrofitProvider.create(url, client).create(AuthApi::class.java))
        assertEquals(AppResult.Failure(AppError.Connection), repo.register(request()))
    }

    @Test
    fun `redirect does not forward registration credentials`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", "/other"))
        assertEquals(AppResult.Failure(AppError.Http(307)), repository().register(request()))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `coroutine cancellation remains cancellation`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val operation = async(Dispatchers.Default) { repository().register(request()) }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        operation.cancelAndJoin()
        assertTrue(operation.isCancelled)
    }

    @Test
    fun `production client has bounded timeouts and no automatic retries`() {
        val configured = NetworkClient.create()
        assertEquals(15_000, configured.connectTimeoutMillis)
        assertEquals(30_000, configured.readTimeoutMillis)
        assertEquals(30_000, configured.writeTimeoutMillis)
        assertEquals(45_000, configured.callTimeoutMillis)
        assertFalse(configured.retryOnConnectionFailure)
        assertFalse(configured.followRedirects)
        assertFalse(configured.followSslRedirects)
    }

    private val successBody = """
        {"success":true,"message":"Registered","data":{
          "accessToken":"test-access","refreshToken":"test-refresh","tokenType":"Bearer",
          "expiresIn":3600,"user":{"id":42,"phoneNumber":"+15555550123",
          "authProvider":"PHONE","role":"USER"}},"timestamp":"2026-10-29T06:40:44.982Z",
          "futureField":"ignored"}
    """.trimIndent()
}
