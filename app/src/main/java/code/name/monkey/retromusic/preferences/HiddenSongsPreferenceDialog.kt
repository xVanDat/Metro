package code.name.monkey.retromusic.preferences

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.fragment.app.DialogFragment
import code.name.monkey.appthemehelper.common.prefs.supportv7.ATEDialogPreference
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.accentTextColor
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.colorControlNormal
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.providers.HiddenSongsStore
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class HiddenSongsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1,
    defStyleRes: Int = -1,
) : ATEDialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            context.colorControlNormal(), SRC_IN
        )
    }
}

class HiddenSongsPreferenceDialog : DialogFragment() {
    private val libraryViewModel by activityViewModel<LibraryViewModel>()
    private lateinit var adapter: ArrayAdapter<String>
    private var entries = emptyList<HiddenSongsStore.HiddenSong>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        refresh()
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels())
        return materialDialog(R.string.hidden_songs)
            .setPositiveButton(R.string.done, null)
            .setNeutralButton(R.string.clear_action, null)
            .setAdapter(adapter) { _, which -> restoreEntry(which) }
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).accentTextColor()
                    getButton(AlertDialog.BUTTON_NEUTRAL).accentTextColor()
                    getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = entries.isNotEmpty()
                    getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { confirmRestoreAll() }
                }
            }
    }

    private fun restoreEntry(which: Int) {
        val entry = entries.getOrNull(which) ?: return
        materialDialog(R.string.hidden_songs)
            .setMessage(R.string.restore_hidden_song)
            .setPositiveButton(R.string.restore_action) { _, _ ->
                HiddenSongsStore.getInstance(requireContext()).restore(entry.id)
                refreshAdapter()
                libraryViewModel.reloadLibraryContent()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create().colorButtons().show()
    }

    private fun confirmRestoreAll() {
        materialDialog(R.string.hidden_songs)
            .setMessage(R.string.clear_hidden_songs)
            .setPositiveButton(R.string.restore_action) { _, _ ->
                HiddenSongsStore.getInstance(requireContext()).clear()
                refreshAdapter()
                libraryViewModel.reloadLibraryContent()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create().colorButtons().show()
    }

    private fun refreshAdapter() {
        refresh()
        adapter.clear()
        adapter.addAll(labels())
        adapter.notifyDataSetChanged()
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = entries.isNotEmpty()
    }

    private fun refresh() {
        entries = HiddenSongsStore.getInstance(requireContext()).entries
    }

    private fun labels(): List<String> = if (entries.isEmpty()) {
        listOf(getString(R.string.no_hidden_songs))
    } else {
        entries.map { it.displayName }
    }

    companion object {
        fun newInstance() = HiddenSongsPreferenceDialog()
    }
}
