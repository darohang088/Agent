package com.daro.agentapp.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.preference.PreferenceManager
import java.util.Locale

object LanguageUtils {
    private const val LANGUAGE_KEY = "app_language"

    fun setLanguage(context: Context, languageCode: String) {
        saveLanguage(context, languageCode)
    }

    fun getLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(LANGUAGE_KEY, Locale.getDefault().language) ?: "en"
    }

    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun checkCurrentLanguageCode(context: Context, newLangCode: String) {
        val currentLangCode = getLanguage(context)
        if (currentLangCode != newLangCode) {
            setLanguage(context, newLangCode)
            recreateActivity(context, newLangCode)
        }
    }

    fun recreateActivity(context: Context, newLangCode: String) {
        val currentActivity = context as? Activity ?: return

        val appContext = currentActivity.applicationContext
        val locale = Locale(newLangCode)
        Locale.setDefault(locale)

        val config = Configuration(appContext.resources.configuration)
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)

        currentActivity.recreate()
    }

    private fun saveLanguage(context: Context, languageCode: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(LANGUAGE_KEY, languageCode).apply()
    }
}