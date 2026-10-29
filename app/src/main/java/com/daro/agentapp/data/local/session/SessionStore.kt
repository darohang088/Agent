package com.daro.agentapp.data.local.session

import kotlinx.serialization.Serializable

internal interface SessionStore {
    suspend fun read(): StoredSession?
    suspend fun write(session: StoredSession)
    suspend fun clear()
}

@Serializable
internal class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresAtEpochMillis: Long,
    val userId: Long,
    val role: String,
) {
    override fun toString() = "StoredSession([REDACTED])"
}
