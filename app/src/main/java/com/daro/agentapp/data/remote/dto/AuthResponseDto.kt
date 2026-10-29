package com.daro.agentapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal class AuthResponseDto(
    val success: Boolean,
    val message: String? = null,
    val data: AuthSessionDto? = null,
    val timestamp: String? = null,
)

@Serializable
internal class AuthSessionDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: AuthUserDto,
)

@Serializable
internal class AuthUserDto(
    val id: Long,
    val phoneNumber: String,
    val authProvider: String,
    val role: String,
)
