package com.daro.agentapp

import android.app.Application
import android.content.Context
import com.daro.agentapp.core.utils.LanguageUtils
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
            environment = EnvironmentConfig(BuildConfig.FLAVOR, BuildConfig.DEBUG, getString(R.string.api_base_url)),
            sessionStore = KeystoreSessionStore(this),
            internetConnectionMonitor = InternetConnectionMonitor(this),
        )
    }
}
