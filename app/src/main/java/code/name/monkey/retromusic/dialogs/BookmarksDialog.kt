package code.name.monkey.retromusic.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.providers.PlaybackPositionStore
import code.name.monkey.retromusic.util.MusicUtil

class BookmarksDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song = MusicPlayerRemote.currentSong
        val bookmarks = PlaybackPositionStore.bookmarksFor(requireContext(), song)
        val dialog = materialDialog(R.string.bookmarks)

        if (bookmarks.isEmpty()) {
            dialog.setMessage(R.string.no_bookmarks)
        } else {
            dialog.setItems(
                bookmarks.map {
                    MusicUtil.getReadableDurationString(it.position.toLong())
                }.toTypedArray()
            ) { _, index ->
                MusicPlayerRemote.seekTo(bookmarks[index].position)
                dismiss()
            }
            dialog.setNegativeButton(R.string.clear_bookmarks) { _, _ ->
                PlaybackPositionStore.clearBookmarks(requireContext(), song)
            }
        }

        dialog.setPositiveButton(R.string.add_bookmark) { _, _ ->
            val added = PlaybackPositionStore.addBookmark(
                requireContext(), song, MusicPlayerRemote.songProgressMillis
            )
            showToast(if (added) R.string.bookmark_added else R.string.bookmark_not_added)
        }
        dialog.setNeutralButton(android.R.string.cancel, null)
        return dialog.create().colorButtons()
    }
}
