package com.daro.agentapp.data.remote

import okhttp3.Interceptor

/** No logger, formatter, or body inspection is included in any release flavor. */
internal object NetworkLogging {
    fun interceptors(): List<Interceptor> = emptyList()
}
