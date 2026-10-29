package com.daro.agentapp

import android.app.Application
import android.content.Context
import com.daro.agentapp.utils.LanguageUtils
import com.daro.agentapp.app.AppContainer
import com.daro.agentapp.core.config.EnvironmentConfig
import com.daro.agentapp.data.local.session.KeystoreSessionStore
import com.daro.agentapp.core.network.InternetConnectionMonitor

class AgentApplication : Application() {
    override fun attachBaseContext(newBase: Context) {
        val language = LanguageUtils.getLanguage(newBase)
        super.attachBaseContext(LanguageUtils.applyLanguage(newBase, language))
    }

    val container: AppContainer by lazy {
        AppContainer(
            environment = EnvironmentConfig(BuildConfig.FLAVOR, BuildConfig.DEBUG, BuildConfig.API_BASE_URL),
            sessionStore = KeystoreSessionStore(this),
            internetConnectionMonitor = InternetConnectionMonitor(this),
        )
    }
}
