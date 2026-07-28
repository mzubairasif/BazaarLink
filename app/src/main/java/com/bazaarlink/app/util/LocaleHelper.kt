package com.bazaarlink.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private var currentLanguage = "en"

    fun getLanguage(): String = currentLanguage

    fun setLocale(context: Context, languageCode: String): Context {
        currentLanguage = languageCode
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
