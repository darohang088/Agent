package com.daro.agentapp.domain.model

/** Authentication response. SessionRepository establishes persistence only when explicitly requested. */
class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: AuthUser,
) {
    override fun toString(): String = "AuthSession([REDACTED])"
}

class AuthUser(
    val id: Long,
    val phoneNumber: String,
    val authProvider: String,
    val role: String,
) {
    override fun toString(): String = "AuthUser([REDACTED])"
}
