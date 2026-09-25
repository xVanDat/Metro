/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import code.name.monkey.retromusic.LANGUAGE_NAME
import java.util.Locale

/**
 * Applies the language selected inside Metro before Android inflates any UI.
 *
 * AppCompat's locale storage and Metro's own preference used to compete with
 * each other. Keeping the preference as the single source of truth also makes
 * switching languages reliable on Android versions below and above Android 13.
 */
object AppLocaleManager {
    private const val AUTO = "auto"
    private const val ENGLISH = "en"
    private const val VIETNAMESE = "vi"

    fun wrap(context: Context): Context {
        val language = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(LANGUAGE_NAME, AUTO)
            .normalizeLanguage()
        val locale = when (language) {
            ENGLISH -> Locale.ENGLISH
            // Use the concrete region as well, so older Android resource
            // matchers select Vietnamese consistently.
            VIETNAMESE -> Locale("vi", "VN")
            else -> return context
        }

        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    fun normalize(language: String?): String = language.normalizeLanguage()

    /** Keeps Android 13's per-app language and AppCompat in sync with Metro. */
    fun syncFrameworkLocale(context: Context, language: String? = null): Boolean {
        val normalized = language?.normalizeLanguage()
            ?: PreferenceManager.getDefaultSharedPreferences(context)
                .getString(LANGUAGE_NAME, AUTO)
                .normalizeLanguage()
        val locales = when (normalized) {
            ENGLISH -> LocaleListCompat.forLanguageTags(ENGLISH)
            VIETNAMESE -> LocaleListCompat.forLanguageTags("vi-VN")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() ==
            locales.toLanguageTags()
        ) {
            return false
        }
        AppCompatDelegate.setApplicationLocales(locales)
        return true
    }

    private fun String?.normalizeLanguage(): String = when (this?.substringBefore('-')) {
        ENGLISH -> ENGLISH
        VIETNAMESE -> VIETNAMESE
        else -> AUTO
    }
}
