package com.daro.agentapp.fixtures

import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.LoginRequest
import com.daro.agentapp.domain.model.RegisterRequest
import com.daro.agentapp.domain.repository.AuthRepository

/** Test-only backend substitute; never part of a runtime or release source set. */
internal class FakeAuthRepository(
    private val loginHandler: suspend (LoginRequest) -> AppResult<AuthSession> = {
        error("Configure a login fake explicitly for login tests")
    },
    private val handler: suspend (RegisterRequest) -> AppResult<AuthSession>,
) : AuthRepository {
    override suspend fun login(request: LoginRequest): AppResult<AuthSession> =
        loginHandler(request)

    override suspend fun register(request: RegisterRequest): AppResult<AuthSession> = handler(request)
}
