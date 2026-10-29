package com.daro.agentapp.data.remote

import android.util.Log
import okhttp3.Interceptor

internal object NetworkLogging {
    fun interceptors(): List<Interceptor> = listOf(
        PrettyJsonLoggingInterceptor { line -> Log.d("AgentNetwork", line) },
    )
}
