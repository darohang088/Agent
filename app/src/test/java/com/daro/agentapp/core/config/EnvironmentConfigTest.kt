package com.daro.agentapp.core.config

import org.junit.Assert.*
import org.junit.Test

class EnvironmentConfigTest {
    @Test fun `all releases reject cleartext`() {
        AppEnvironment.entries.forEach {
            assertFalse(EnvironmentConfig(it.flavor, false, "http://43.224.34.135:3003/").isConfigured)
        }
    }
    @Test fun `production requires an explicit valid https url`() {
        assertTrue(EnvironmentConfig("prod", false, "https://43.224.34.135:3003/").isConfigured)
        for (url in listOf("", "https://user:secret@example.com/", "https://example.com/?token=secret",
            "https://example.com/#secret", "https://example.com/api")) {
            assertFalse(EnvironmentConfig("prod", false, url).isConfigured)
        }
    }
    @Test fun `debug cleartext is restricted to the supplied development host`() {
        AppEnvironment.entries.forEach {
            assertTrue(EnvironmentConfig(it.flavor, true, "http://43.224.34.135:3003/").isConfigured)
            assertFalse(EnvironmentConfig(it.flavor, true, "http://example.com/").isConfigured)
        }
    }
    @Test fun `unknown environment has no production fallback`() {
        assertFalse(EnvironmentConfig("unknown", false, "https://example.com/").isConfigured)
    }
}
