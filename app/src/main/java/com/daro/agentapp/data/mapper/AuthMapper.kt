package com.daro.agentapp.data.mapper

import com.daro.agentapp.data.remote.dto.AuthSessionDto
import com.daro.agentapp.data.remote.dto.LoginRequestDto
import com.daro.agentapp.data.remote.dto.RegisterRequestDto
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.LoginRequest
import com.daro.agentapp.domain.model.AuthUser
import com.daro.agentapp.domain.model.RegisterRequest

internal fun LoginRequest.toDto() = LoginRequestDto(phoneNumber, password)

internal fun RegisterRequest.toDto() = RegisterRequestDto(
    phoneNumber, password, firstName, lastName, displayName, dateBirth, gender,
    latitude, longitude, locationName, interested,
)

internal fun AuthSessionDto.toDomain() = AuthSession(
    accessToken, refreshToken, tokenType, expiresIn,
    AuthUser(user.id, user.phoneNumber, user.authProvider, user.role),
)
