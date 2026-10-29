package com.daro.agentapp.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences as AndroidEncryptedPreferences
import androidx.security.crypto.MasterKeys

/** Initialized lazily when needed; cryptographic failures propagate rather than using plaintext. */
object EncryptedSharedPreferences {
    @Volatile private var preferences: SharedPreferences? = null

    fun getInstance(): SharedPreferences = checkNotNull(preferences) {
        "Encrypted preferences must be initialized before use."
    }

    fun isInit(): Boolean = preferences != null

    @Synchronized
    fun init(context: Context) {
        if (preferences != null) return
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        preferences = AndroidEncryptedPreferences.create(
            "biz_shared_prefs", masterKey, context.applicationContext,
            AndroidEncryptedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            AndroidEncryptedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
