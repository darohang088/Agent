package com.daro.agentapp.core.utils

import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.AuthUser
import com.daro.agentapp.utils.toPrettyJson
import org.junit.Assert.*
import org.junit.Test

class DebugJsonLogTest {
    @Test fun explicitDebugLogEmitsRawResponseValues() {
        val session = AuthSession("synthetic-access", "synthetic-refresh", "Bearer", 3600,
            AuthUser(7, "+15555550123", "PHONE", "USER"))
        val lines = mutableListOf<String>()
        DebugJsonLog.emit({ session.toPrettyJson() }, lines::add)
        val output = lines.joinToString("\n")
        assertTrue(output.contains("synthetic-access"))
        assertTrue(output.contains("synthetic-refresh"))
        assertTrue(output.contains("+15555550123"))
    }

    @Test fun formatterAndSinkFailuresCannotBreakLogin() {
        DebugJsonLog.emit({ throw IllegalStateException("format failed") }, {})
        DebugJsonLog.emit({ "{}" }) { throw IllegalStateException("sink failed") }
    }
}
