package com.daro.agentapp.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootDetectionTest {
    @Test
    fun visibleSuIsReported() {
        val result = RootDetection.inspect("release-keys") { it == "/system/xbin/su" }
        assertTrue(result.isRootSuspected)
        assertEquals(listOf("/system/xbin/su"), result.detectedPaths)
    }

    @Test
    fun testKeysAloneDoNotIndicateRoot() {
        val result = RootDetection.inspect("test-keys") { false }
        assertTrue(result.hasTestKeys)
        assertFalse(result.isRootSuspected)
    }

    @Test
    fun unavailableTagsAndNoArtifactsDoNotIndicateRoot() {
        val result = RootDetection.inspect(null) { false }
        assertFalse(result.hasTestKeys)
        assertFalse(result.isRootSuspected)
    }

    @Test
    fun deniedPathIsReportedAndOtherChecksContinue() {
        val result = RootDetection.inspect("release-keys") {
            when (it) {
                "/system/bin/su" -> throw SecurityException("Denied")
                "/system/xbin/su" -> true
                else -> false
            }
        }
        assertEquals(listOf("/system/bin/su"), result.inaccessiblePaths)
        assertTrue(result.isRootSuspected)
    }
}
