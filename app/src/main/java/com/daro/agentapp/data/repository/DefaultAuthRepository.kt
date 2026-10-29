package com.daro.agentapp.data.repository

import com.daro.agentapp.base.BaseRepo
import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.data.mapper.toDomain
import com.daro.agentapp.data.mapper.toDto
import com.daro.agentapp.data.remote.api.AuthApi
import com.daro.agentapp.data.remote.dto.AuthResponseDto
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.LoginRequest
import com.daro.agentapp.domain.model.RegisterRequest
import com.daro.agentapp.domain.repository.AuthRepository

internal class DefaultAuthRepository(private val api: AuthApi) : BaseRepo(), AuthRepository {
    override suspend fun login(request: LoginRequest): AppResult<AuthSession> {
        if (request.phoneNumber.isBlank() || request.password.isEmpty()) {
            return AppResult.Failure(AppError.InvalidInput)
        }
        return sessionResult(apiCall { api.login(request.toDto()) })
    }

    override suspend fun register(request: RegisterRequest): AppResult<AuthSession> {
        if (!request.latitude.isFinite() || request.latitude !in -90.0..90.0 ||
            !request.longitude.isFinite() || request.longitude !in -180.0..180.0
        ) return AppResult.Failure(AppError.InvalidInput)
        return sessionResult(apiCall { api.register(request.toDto()) })
    }

    private fun sessionResult(result: AppResult<AuthResponseDto>): AppResult<AuthSession> = when (result) {
        is AppResult.Failure -> result
        is AppResult.Success -> {
            val response = result.value
            val session = response.data
            when {
                !response.success -> AppResult.Failure(AppError.Rejected)
                session == null || session.accessToken.isBlank() || session.refreshToken.isBlank() ||
                    session.tokenType.isBlank() || session.expiresIn < 0 ->
                    AppResult.Failure(AppError.InvalidResponse)
                else -> AppResult.Success(session.toDomain())
            }
        }
    }
}
