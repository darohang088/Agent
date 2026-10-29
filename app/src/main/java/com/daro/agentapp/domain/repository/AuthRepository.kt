package com.daro.agentapp.domain.repository

import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.LoginRequest
import com.daro.agentapp.domain.model.RegisterRequest

interface AuthRepository {
    /** Authenticates and returns credentials; SessionRepository explicitly owns persistence. */
    suspend fun login(request: LoginRequest): AppResult<AuthSession>

    /** Creates an account. Never retry automatically after an ambiguous network failure. */
    suspend fun register(request: RegisterRequest): AppResult<AuthSession>
}
