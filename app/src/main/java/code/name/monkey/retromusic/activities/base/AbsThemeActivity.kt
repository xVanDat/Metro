/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.activities.base

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.common.ATHToolbarActivity
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.LANGUAGE_NAME
import code.name.monkey.retromusic.extensions.*
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.CustomFontManager
import code.name.monkey.retromusic.util.FontApplier
import code.name.monkey.retromusic.util.theme.getNightMode
import code.name.monkey.retromusic.util.theme.getThemeResValue

abstract class AbsThemeActivity : ATHToolbarActivity(), Runnable {

    private val handler = Handler(Looper.getMainLooper())
    private var fontApplier: FontApplier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        updateLocale()
        updateTheme()
        hideStatusBar()
        super.onCreate(savedInstanceState)
        fontApplier = CustomFontManager.attach(window.decorView)
        setEdgeToEdgeOrImmersive()
        maybeSetScreenOn()
        setLightNavigationBarAuto()
        setLightStatusBarAuto(surfaceColor())
        if (VersionUtils.hasQ()) {
            window.decorView.isForceDarkAllowed = false
        }
    }

    private fun updateTheme() {
        setTheme(getThemeResValue())
        if (PreferenceUtil.materialYou) {
            setDefaultNightMode(getNightMode())
        }

        if (PreferenceUtil.isCustomFont && !CustomFontManager.hasCustomFont(this)) {
            setTheme(R.style.FontThemeOverlay)
        }
    }

    private fun updateLocale() {
        val localeCode = PreferenceUtil.languageCode
            .takeIf { it == "auto" || it.substringBefore('-') in SUPPORTED_LANGUAGES }
            ?: "auto"
        if (PreferenceUtil.languageCode != localeCode) {
            PreferenceUtil.languageCode = localeCode
        }

        val targetLocales = if (localeCode == "auto") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeCode)
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() !=
            targetLocales.toLanguageTags()
        ) {
            AppCompatDelegate.setApplicationLocales(targetLocales)
        }
        PreferenceUtil.isLocaleAutoStorageEnabled = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
            handler.removeCallbacks(this)
            handler.postDelayed(this, 300)
        } else {
            handler.removeCallbacks(this)
        }
    }

    override fun run() {
        setImmersiveFullscreen()
    }

    override fun onStop() {
        handler.removeCallbacks(this)
        super.onStop()
    }

    public override fun onDestroy() {
        fontApplier?.stop()
        fontApplier = null
        super.onDestroy()
        exitFullscreen()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            handler.removeCallbacks(this)
            handler.postDelayed(this, 500)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(null)
            return
        }
        val languageCode = PreferenceManager.getDefaultSharedPreferences(newBase)
            .getString(LANGUAGE_NAME, "auto")
            ?.substringBefore('-')
        val localizedContext = if (languageCode in SUPPORTED_LANGUAGES) {
            val locale = java.util.Locale.forLanguageTag(languageCode!!)
            val configuration = Configuration(newBase.resources.configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            newBase.createConfigurationContext(configuration)
        } else {
            newBase
        }
        super.attachBaseContext(localizedContext)
    }

    companion object {
        private val SUPPORTED_LANGUAGES = setOf("en", "vi")
    }
}
