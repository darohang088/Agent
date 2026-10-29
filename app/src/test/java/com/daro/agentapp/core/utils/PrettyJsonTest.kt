package com.daro.agentapp.core.utils

import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.AuthUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class PrettyJsonTest {
    @Serializable private data class ExampleResponse(val status: String)

    @Test fun `explicit session formatter includes actual values and nested user data`() {
        val session = AuthSession("synthetic-access", "synthetic-refresh", "Bearer", 3600,
            AuthUser(7, "+15555550123", "PHONE", "USER"))
        val output = session.toPrettyJson()
        val data = Json.parseToJsonElement(output).jsonObject
        assertEquals("synthetic-access", data.getValue("accessToken").jsonPrimitive.content)
        assertEquals("synthetic-refresh", data.getValue("refreshToken").jsonPrimitive.content)
        assertEquals("+15555550123", data.getValue("user").jsonObject.getValue("phoneNumber").jsonPrimitive.content)
        assertTrue(output.contains("\n    \"accessToken\""))
        assertFalse(data.containsKey("password"))
        // Default string conversion stays redacted; raw formatting requires an explicit call.
        assertFalse(session.toString().contains("synthetic-access"))
    }

    @Test fun `json strings and serializable models format as objects`() {
        assertTrue("{\"success\":true}".toPrettyJson().contains("\n    \"success\": true"))
        val output = ExampleResponse("ok").toPrettyJson()
        assertEquals("ok", Json.parseToJsonElement(output).jsonObject.getValue("status").jsonPrimitive.content)
    }
}
