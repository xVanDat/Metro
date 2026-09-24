/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.preferences

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.fragment.app.DialogFragment
import code.name.monkey.appthemehelper.common.prefs.supportv7.ATEDialogPreference
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.colorControlNormal
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.fragments.NowPlayingScreen.*
import code.name.monkey.retromusic.util.PreferenceUtil

class NowPlayingScreenPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1,
    defStyleRes: Int = -1
) : ATEDialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            context.colorControlNormal(),
            SRC_IN
        )
    }
}

class NowPlayingScreenPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var selectedPosition = PreferenceUtil.nowPlayingScreen.ordinal
        val entries = values().map { getString(it.titleRes) }.toTypedArray()

        return materialDialog(R.string.pref_title_now_playing_screen_appearance)
            .setSingleChoiceItems(entries, selectedPosition) { _, position ->
                selectedPosition = position
            }
            .setPositiveButton(R.string.set) { _, _ ->
                val nowPlayingScreen = values()[selectedPosition]
                PreferenceUtil.nowPlayingScreen = nowPlayingScreen
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
            .colorButtons()
    }

    companion object {
        fun newInstance(): NowPlayingScreenPreferenceDialog {
            return NowPlayingScreenPreferenceDialog()
        }
    }
}
