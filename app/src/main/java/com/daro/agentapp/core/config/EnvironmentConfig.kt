package com.daro.agentapp.core.config

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class AppEnvironment(val flavor: String) {
    Staging("staging"), Uat("uat"), PreProd("preProd"), Prod("prod");

    companion object {
        fun fromFlavor(value: String): AppEnvironment? = entries.firstOrNull { it.flavor == value }
    }
}

/** Unknown/missing environments never fall back to production. No URL credentials or queries. */
class EnvironmentConfig(val flavor: String, val isDebug: Boolean, baseUrl: String) {
    val environment = AppEnvironment.fromFlavor(flavor)
    val baseUrl: HttpUrl? = baseUrl.trim().toHttpUrlOrNull()?.takeIf { url ->
        environment != null && url.username.isEmpty() && url.password.isEmpty() &&
            url.query == null && url.fragment == null && url.encodedPath.endsWith('/') &&
            (url.isHttps || (isDebug && url.host == DEVELOPMENT_HOST))
    }
    val isConfigured: Boolean get() = baseUrl != null

    companion object { const val DEVELOPMENT_HOST = "43.224.34.135" }
}
