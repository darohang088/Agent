package com.daro.agentapp

import android.app.Application
import android.content.Context
import com.daro.agentapp.di.ApplicationScope
import com.daro.agentapp.domain.session.SessionRepository
import com.daro.agentapp.utils.LanguageUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class AgentApplication : Application() {
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageUtils.getLanguage(newBase)
        super.attachBaseContext(LanguageUtils.applyLanguage(newBase, language))
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { sessionRepository.restore() }
    }
}
