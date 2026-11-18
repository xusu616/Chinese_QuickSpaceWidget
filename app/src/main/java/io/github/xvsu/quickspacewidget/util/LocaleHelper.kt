package io.github.xvsu.quickspacewidget.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import io.github.xvsu.quickspacewidget.model.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun resolveLocale(language: AppLanguage, systemContext: Context? = null): Locale = when (language) {
        AppLanguage.ZH_CN -> Locale.SIMPLIFIED_CHINESE
        AppLanguage.EN -> Locale.ENGLISH
        AppLanguage.SYSTEM -> {
            val sysLocale = systemContext?.resources?.configuration?.locales?.get(0)
                ?: LocaleList.getDefault().get(0)
                ?: Locale.getDefault()
            // Resources ship values-zh-rCN and English default; normalize all Chinese locales to zh-CN
            if (sysLocale.language.equals("zh", ignoreCase = true)) {
                Locale.SIMPLIFIED_CHINESE
            } else {
                Locale.ENGLISH
            }
        }
    }

    @SuppressLint("AppBundleLocaleChanges")
    fun localizedContext(base: Context, language: AppLanguage): Context {
        val targetLocale = resolveLocale(language, base)
        val currentLocale = base.resources.configuration.locales.get(0)
        if (language == AppLanguage.SYSTEM &&
            currentLocale != null &&
            currentLocale.language == targetLocale.language &&
            currentLocale.country == targetLocale.country
        ) {
            return base
        }
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(targetLocale))
        return base.createConfigurationContext(config)
    }
}
