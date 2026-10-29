package com.daro.agentapp.core.utils

/** Release never evaluates the raw-value formatting lambda or emits a Logcat entry. */
@Suppress("UNUSED_PARAMETER")
internal object DebugJsonLog {
    inline fun d(tag: String, json: () -> String) = Unit
}
