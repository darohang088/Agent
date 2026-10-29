package com.daro.agentapp.domain.session

import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.domain.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val state: StateFlow<SessionState>
    suspend fun restore()
    suspend fun establish(session: AuthSession): AppResult<Unit>
    suspend fun logout(): AppResult<Unit>
    /** For a future authenticated transport; never put this value in UI or saved state. */
    suspend fun accessToken(): String?
}

/** Register feature-owned user caches for cleanup when a session ends. */
fun interface SessionDataCleaner {
    suspend fun clearUserData()
}
