package com.daro.agentapp.app

import com.daro.agentapp.core.config.EnvironmentConfig
import com.daro.agentapp.core.network.InternetConnectionMonitor
import com.daro.agentapp.data.local.session.SessionStore
import com.daro.agentapp.data.remote.NetworkClient
import com.daro.agentapp.data.remote.NetworkLogging
import com.daro.agentapp.data.remote.RetrofitProvider
import com.daro.agentapp.data.remote.api.AuthApi
import com.daro.agentapp.data.repository.DefaultAuthRepository
import com.daro.agentapp.data.repository.DefaultGreetingRepository
import com.daro.agentapp.data.repository.DefaultSessionRepository
import com.daro.agentapp.data.repository.UnconfiguredAuthRepository
import com.daro.agentapp.domain.repository.AuthRepository
import com.daro.agentapp.domain.repository.GreetingRepository
import com.daro.agentapp.domain.session.SessionDataCleaner
import com.daro.agentapp.domain.session.SessionRepository
import com.daro.agentapp.presentation.greeting.GreetingViewModel
import com.daro.agentapp.presentation.register.RegisterViewModel
import com.daro.agentapp.presentation.login.LoginViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer internal constructor(
    val environment: EnvironmentConfig,
    val internetConnectionMonitor: InternetConnectionMonitor,
    sessionStore: SessionStore,
    userDataCleaners: List<SessionDataCleaner> = emptyList(),
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val sessionRepository: SessionRepository = DefaultSessionRepository(
        sessionStore, applicationScope, userDataCleaners,
    ).also { repository -> applicationScope.launch { repository.restore() } }

    private val httpClient by lazy { NetworkClient.create(allowDevelopmentHttp = environment.isDebug) }
    private val retrofit by lazy {
        RetrofitProvider.create(checkNotNull(environment.baseUrl).toString(), httpClient, NetworkLogging.interceptors())
    }
    private val authApi by lazy { retrofit.create(AuthApi::class.java) }
    val authRepository: AuthRepository by lazy {
        if (environment.isConfigured) DefaultAuthRepository(authApi) else UnconfiguredAuthRepository()
    }
    val loginViewModelFactory by lazy { LoginViewModel.factory(authRepository, sessionRepository) }
    val registerViewModelFactory by lazy { RegisterViewModel.factory(authRepository) }
    private val greetingRepository: GreetingRepository = DefaultGreetingRepository()
    val greetingViewModelFactory = GreetingViewModel.factory(greetingRepository)
}
