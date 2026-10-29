package com.daro.agentapp.core.utils

import android.util.Log

/** Explicit opt-in to raw response values, compiled for every debug flavor only. */
internal object DebugJsonLog {
    private val lock = Any()

    fun d(tag: String, json: () -> String) = emit(json) { line -> Log.d(tag, line) }

    internal fun emit(json: () -> String, sink: (String) -> Unit) {
        try {
            val message = json()
            synchronized(lock) {
                message.lineSequence().forEach { line ->
                    line.chunked(900).ifEmpty { listOf("") }.forEach(sink)
                }
            }
        } catch (_: Exception) {
            // Formatting or Logcat failures must not change the login outcome.
        }
    }
}
