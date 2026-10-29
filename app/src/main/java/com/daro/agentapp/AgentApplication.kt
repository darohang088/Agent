package com.daro.agentapp

import android.app.Application
import com.daro.agentapp.app.AppContainer

class AgentApplication : Application() {
    val container: AppContainer by lazy { AppContainer() }
}
