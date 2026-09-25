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
package code.name.monkey.retromusic.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.fragments.LibraryViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ImportPlaylistDialog : DialogFragment() {
    private val libraryViewModel by activityViewModel<LibraryViewModel>()
    private val playlistFilePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        libraryViewModel.importPlaylistFile(requireContext(), uri)
        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return materialDialog(R.string.import_playlist)
            .setMessage(R.string.import_playlist_source_message)
            .setPositiveButton(R.string.choose_playlist_file, null)
            .setNeutralButton(R.string.import_from_media_store, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener {
                    colorButtons()
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        // Playlist MIME types vary widely between file managers.
                        // Showing every file keeps M3U, PLS, XSPF, WPL and ASX selectable.
                        playlistFilePicker.launch(arrayOf("*/*"))
                    }
                    getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        libraryViewModel.importPlaylists()
                        dismiss()
                    }
                }
            }
    }
}
