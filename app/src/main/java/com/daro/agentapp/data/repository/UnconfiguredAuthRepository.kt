package com.daro.agentapp.data.repository

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.LoginRequest
import com.daro.agentapp.domain.model.RegisterRequest
import com.daro.agentapp.domain.repository.AuthRepository

/** A missing release URL produces a safe result instead of a crash or development-server fallback. */
internal class UnconfiguredAuthRepository : AuthRepository {
    override suspend fun login(request: LoginRequest): AppResult<AuthSession> =
        AppResult.Failure(AppError.Configuration)

    override suspend fun register(request: RegisterRequest): AppResult<AuthSession> =
        AppResult.Failure(AppError.Configuration)
}
