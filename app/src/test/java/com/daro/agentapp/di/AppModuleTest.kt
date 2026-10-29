package com.daro.agentapp.di

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.core.config.EnvironmentConfig
import com.daro.agentapp.data.remote.api.AuthApi
import com.daro.agentapp.domain.model.LoginRequest
import javax.inject.Provider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppModuleTest {
    @Test fun invalidReleaseEnvironmentDoesNotResolveApiAndReturnsConfigurationFailure() = runTest {
        val environment = EnvironmentConfig("staging", false, "http://43.224.34.135:3003/")
        val repository = AppModule.authRepository(environment, Provider<AuthApi> {
            error("Invalid environment must not construct Retrofit or resolve AuthApi")
        })
        assertEquals(
            AppResult.Failure(AppError.Configuration),
            repository.login(LoginRequest("+15555550123", "synthetic-password")),
        )
    }
}
