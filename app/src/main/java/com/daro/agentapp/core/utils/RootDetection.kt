package com.daro.agentapp.core.utils

import android.os.Build
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local heuristics only: hidden root can evade detection. No root commands are executed. */
object RootDetection {
    /** True means a known root artifact was visible, not that root access was proven. */
    suspend fun isDeviceRooted(): Boolean = check().isRootSuspected

    /** Disk checks run off the main thread. Test keys alone do not classify a device as rooted. */
    suspend fun check(): RootCheckResult = withContext(Dispatchers.IO) {
        inspect(Build.TAGS) { path -> File(path).exists() }
    }

    internal fun inspect(
        buildTags: String?,
        exists: (String) -> Boolean,
    ): RootCheckResult {
        val detectedPaths = mutableListOf<String>()
        val inaccessiblePaths = mutableListOf<String>()
        for (path in rootArtifactPaths) {
            try {
                if (exists(path)) detectedPaths += path
            } catch (_: SecurityException) {
                inaccessiblePaths += path
            }
        }
        return RootCheckResult(
            detectedPaths = detectedPaths.toList(),
            hasTestKeys = buildTags?.split(',')?.any { it.trim() == "test-keys" } == true,
            inaccessiblePaths = inaccessiblePaths.toList(),
        )
    }

    private val rootArtifactPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/system/sbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/su/bin/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/sbin/.magisk",
        "/data/adb/magisk",
    )
}

/** No visible artifacts does not prove a device is unrooted; filesystem checks can be hidden. */
data class RootCheckResult(
    val detectedPaths: List<String>,
    val hasTestKeys: Boolean,
    val inaccessiblePaths: List<String>,
) {
    val isRootSuspected: Boolean get() = detectedPaths.isNotEmpty()
}
