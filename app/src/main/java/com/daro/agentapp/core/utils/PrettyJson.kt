package com.daro.agentapp.core.utils

import com.daro.agentapp.domain.model.AuthSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Explicit raw-value formatting. Never pass credential JSON to release logs or analytics. */
@PublishedApi
internal val prettyJsonFormat = Json { prettyPrint = true; encodeDefaults = true }

/** Supports models annotated with kotlinx.serialization.Serializable. */
inline fun <reified T> T.toPrettyJson(): String = prettyJsonFormat.encodeToString(this)

/** Formats an existing JSON document, not the string returned by an object's toString(). */
fun String.toPrettyJson(): String = prettyJsonFormat.parseToJsonElement(this).toPrettyJson()

fun JsonElement.toPrettyJson(): String = prettyJsonFormat.encodeToString(JsonElement.serializer(), this)

/** Domain models remain free of serialization annotations; all response data fields are explicit. */
fun AuthSession.toPrettyJson(): String = buildJsonObject {
    put("accessToken", accessToken)
    put("refreshToken", refreshToken)
    put("tokenType", tokenType)
    put("expiresIn", expiresIn)
    put("user", buildJsonObject {
        put("id", user.id)
        put("phoneNumber", user.phoneNumber)
        put("authProvider", user.authProvider)
        put("role", user.role)
    })
}.toPrettyJson()
