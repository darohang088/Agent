package com.daro.agentapp.di

import android.content.Context
import com.daro.agentapp.BuildConfig
import com.daro.agentapp.core.config.EnvironmentConfig
import com.daro.agentapp.core.network.InternetConnectionMonitor
import com.daro.agentapp.data.local.session.KeystoreSessionStore
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Application-owned dependencies. ViewModels are constructed by Hilt in destination scope. */
@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    @Provides @Singleton
    fun environment(): EnvironmentConfig =
        EnvironmentConfig(BuildConfig.FLAVOR, BuildConfig.DEBUG, BuildConfig.API_BASE_URL)

    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun internetMonitor(@ApplicationContext context: Context): InternetConnectionMonitor =
        InternetConnectionMonitor(context)

    @Provides @Singleton
    fun sessionStore(@ApplicationContext context: Context): SessionStore = KeystoreSessionStore(context)

    @Provides @Singleton
    fun sessionRepository(
        store: SessionStore,
        @ApplicationScope scope: CoroutineScope,
        cleaners: Set<@JvmSuppressWildcards SessionDataCleaner>,
    ): SessionRepository = DefaultSessionRepository(store, scope, cleaners.toList())

    @Provides @Singleton
    fun client(environment: EnvironmentConfig): OkHttpClient =
        NetworkClient.create(allowDevelopmentHttp = environment.isDebug)

    @Provides @Singleton
    fun retrofit(environment: EnvironmentConfig, client: OkHttpClient): Retrofit =
        RetrofitProvider.create(checkNotNull(environment.baseUrl).toString(), client, NetworkLogging.interceptors())

    @Provides @Singleton
    fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun authRepository(environment: EnvironmentConfig, api: Provider<AuthApi>): AuthRepository =
        // Do not construct Retrofit for invalid environments; preserve the fail-closed repository.
        if (environment.isConfigured) DefaultAuthRepository(api.get()) else UnconfiguredAuthRepository()

    @Provides @Singleton
    fun greetingRepository(): GreetingRepository = DefaultGreetingRepository()
}

/** Features may contribute user-cache cleanup via @IntoSet without changing session ownership. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionCleanupModule {
    @Multibinds
    abstract fun cleaners(): Set<SessionDataCleaner>
}
