package com.daro.agentapp.core.utils

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import java.util.UUID

object DeviceUtils {
    /** App-install identifier, not a hardware identifier. Not transmitted automatically. */
    fun getDeviceInfo(context: Context): DeviceInfo {
        EncryptedSharedPreferences.init(context.applicationContext)
        val prefs = EncryptedSharedPreferences.getInstance()
        val softwareId = synchronized(this) {
            prefs.getString("software_id", null) ?: UUID.randomUUID().toString().also {
                prefs.edit { putString("software_id", it) }
            }
        }
        return DeviceInfo(Build.DEVICE, Build.MANUFACTURER, Build.MODEL,
            softwareId, "Android ${Build.VERSION.RELEASE}")
    }
}

data class DeviceInfo(
    val deviceName: String,
    val deviceBrand: String,
    val deviceModel: String,
    val softwareId: String,
    val deviceOperationSystem: String,
) {
    override fun toString() = "DeviceInfo([REDACTED])"
}
