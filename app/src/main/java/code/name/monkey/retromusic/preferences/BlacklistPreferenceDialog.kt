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
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.core.text.parseAsHtml
import androidx.fragment.app.DialogFragment
import code.name.monkey.appthemehelper.common.prefs.supportv7.ATEDialogPreference
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.accentTextColor
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.colorControlNormal
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.providers.BlacklistStore
import code.name.monkey.retromusic.util.toLocalStorageFile
import java.io.File

class BlacklistPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1,
    defStyleRes: Int = -1,
) : ATEDialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        icon?.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                context.colorControlNormal(),
                SRC_IN
            )
    }
}

class BlacklistPreferenceDialog : DialogFragment() {
    companion object {
        fun newInstance(): BlacklistPreferenceDialog {
            return BlacklistPreferenceDialog()
        }
    }

    private lateinit var paths: ArrayList<String>
    private lateinit var pathAdapter: ArrayAdapter<String>

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val folder = uri.toLocalStorageFile(requireContext())
        if (folder == null) {
            showToast(R.string.blacklist_folder_not_supported)
            return@registerForActivityResult
        }

        if (BlacklistStore.getInstance(requireContext()).addPath(folder)) {
            refreshBlacklistData(requireContext())
            showToast(R.string.blacklist_folder_added)
        } else {
            showToast(R.string.blacklist_folder_already_added)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireActivity()

        refreshBlacklistData(context)
        pathAdapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, paths)
        return materialDialog(R.string.blacklist)
            .setPositiveButton(R.string.done) { _, _ ->
                dismiss()
            }
            .setNeutralButton(R.string.clear_action, null)
            .setNegativeButton(R.string.add_action, null)
            .setAdapter(pathAdapter) { _, which ->
                materialDialog(R.string.remove_from_blacklist)
                    .setMessage(
                        String.format(
                            getString(R.string.do_you_want_to_remove_from_the_blacklist),
                            paths[which]
                        ).parseAsHtml()
                    )
                    .setPositiveButton(R.string.remove_action) { _, _ ->
                        BlacklistStore.getInstance(context).removePath(File(paths[which]))
                        refreshBlacklistData(context)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .colorButtons()
                    .show()
            }
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).accentTextColor()
                    getButton(AlertDialog.BUTTON_NEGATIVE).accentTextColor()
                    getButton(AlertDialog.BUTTON_NEUTRAL).accentTextColor()
                    getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                        folderPicker.launch(null)
                    }
                    getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        materialDialog(R.string.clear_blacklist)
                            .setMessage(R.string.do_you_want_to_clear_the_blacklist)
                            .setPositiveButton(R.string.clear_action) { _, _ ->
                                BlacklistStore.getInstance(context).clear()
                                refreshBlacklistData(context)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .colorButtons()
                            .show()
                    }
                }
            }
    }

    private fun refreshBlacklistData(context: Context?) {
        if (context == null) return
        this.paths = BlacklistStore.getInstance(context).paths
        if (::pathAdapter.isInitialized) {
            pathAdapter.clear()
            pathAdapter.addAll(paths)
            pathAdapter.notifyDataSetChanged()
        }
    }
}
