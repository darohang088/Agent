package com.daro.agentapp.data.remote

import kotlinx.serialization.json.*
import retrofit2.http.*

/** Sanitizes all content before it crosses into the debug Logcat sink. */
internal class NetworkLogFormatter {
    private val json = Json { prettyPrint = true }

    fun format(
        request: okhttp3.Request,
        requestBody: String?,
        response: okhttp3.Response?,
        responseBody: String?,
        elapsedMs: Long,
        failure: String? = null,
    ): String {
        val invocation = request.tag(retrofit2.Invocation::class.java)
        val service = invocation?.method()?.declaringClass?.simpleName ?: "HTTP"
        val method = invocation?.method()?.name ?: request.method
        val endpoint = invocation?.method()?.annotations?.firstNotNullOfOrNull { annotation ->
            when (annotation) {
                is GET -> annotation.value
                is POST -> annotation.value
                is PUT -> annotation.value
                is PATCH -> annotation.value
                is DELETE -> annotation.value
                is HEAD -> annotation.value
                is OPTIONS -> annotation.value
                is HTTP -> annotation.path
                else -> null
            }
        }?.takeIf { it.isNotBlank() } ?: "[dynamic endpoint]"
        val id = request.header("X-Correlation-ID")
            ?.takeIf { it.matches(Regex("[a-fA-F0-9-]{36}")) } ?: "local"
        return buildString {
            appendLine("================ HTTP [$id] ================")
            appendLine("service = $service")
            appendLine("method = ${request.method} $method")
            // Credentials and query parameters are intentionally excluded.
            appendLine("server = ${request.url.scheme}://${request.url.host}:${request.url.port}")
            appendLine("endpoint = ${endpoint.substringBefore('?')}")
            appendLine("request headers = ${safeHeaders(request.headers)}")
            appendLine("request =")
            appendLine(prettyBody(requestBody))
            appendLine("status = ${response?.code ?: "unavailable"}")
            appendLine("duration = ${elapsedMs}ms")
            if (failure != null) appendLine("failure = $failure")
            appendLine("response headers = ${response?.let { safeHeaders(it.headers) } ?: "{}"}")
            appendLine("response =")
            appendLine(prettyBody(responseBody))
            append("============================================")
        }
    }

    private fun safeHeaders(headers: okhttp3.Headers): String = json.encodeToString(
        JsonElement.serializer(),
        JsonObject(headers.names().associateWith { name ->
            val value = headers[name].orEmpty()
            val safe = when {
                name.equals("Content-Type", true) ->
                    value.matches(Regex("(application/json|text/plain)(; *charset=utf-8)?", RegexOption.IGNORE_CASE))
                name.equals("Accept", true) -> value == "application/json"
                name.equals("X-Correlation-ID", true) -> value.matches(Regex("[a-fA-F0-9-]{36}"))
                else -> false
            }
            JsonPrimitive(if (safe) value else "[REDACTED]")
        }),
    )

    private fun prettyBody(body: String?): String {
        if (body.isNullOrBlank()) return "[body unavailable or empty]"
        if (body.length > 16_384) return "[body omitted: exceeds logging limit]"
        return try {
            json.encodeToString(JsonElement.serializer(), sanitize(json.parseToJsonElement(body)))
        } catch (_: Exception) {
            "[body omitted: invalid or non-JSON content]"
        }
    }

    private fun sanitize(element: JsonElement, key: String? = null, depth: Int = 0): JsonElement {
        if (depth > 16) return JsonPrimitive("[REDACTED]")
        return when (element) {
            is JsonObject -> JsonObject(element.mapValues { (name, value) -> sanitize(value, name, depth + 1) })
            is JsonArray -> JsonArray(element.map { sanitize(it, depth = depth + 1) })
            is JsonPrimitive -> if (isSafeScalar(key, element)) element else JsonPrimitive("[REDACTED]")
        }
    }

    private fun isSafeScalar(key: String?, value: JsonPrimitive): Boolean = when (key) {
        "success" -> !value.isString && value.booleanOrNull != null
        "expiresIn" -> !value.isString && value.longOrNull != null
        "tokenType" -> value.content.equals("Bearer", ignoreCase = true)
        "role" -> value.content in setOf("USER", "ADMIN")
        "authProvider" -> value.content in setOf("PHONE", "GOOGLE", "APPLE")
        "gender" -> value.content in setOf("MALE", "FEMALE", "OTHER")
        else -> false
    }
}
