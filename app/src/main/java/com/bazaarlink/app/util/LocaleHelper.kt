package com.bazaarlink.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "bazaarlink_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    fun getLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLocale(context: Context, languageCode: String): Context {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        val updatedContext = applyLocale(context, languageCode)

        findActivity(context)?.let { activity ->
            activity.recreate()
        }

        return updatedContext
    }

    fun applyLocale(context: Context, languageCode: String = getLanguage(context)): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        if (languageCode == "ur") {
            config.setLayoutDirection(locale)
        } else {
            config.setLayoutDirection(Locale.ENGLISH)
        }

        return context.createConfigurationContext(config)
    }


    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}
