/*
 * Copyright (c) 2026 Metro contributors.
 * Licensed under the GNU General Public License v3.
 */
package code.name.monkey.retromusic.dialogs

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.surfaceColor
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors

/** Full-screen replacement for the narrow anchored player overflow popup. */
class PlayerMenuDialog : DialogFragment(R.layout.dialog_player_menu) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_RetroMusic_PlayerMenu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { dismiss() }
        }

        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.menu_player, popupMenu.menu)
        popupMenu.menu.findItem(R.id.action_toggle_lyrics)?.apply {
            isVisible = requireArguments().getBoolean(ARG_SHOW_LYRICS)
            isChecked = requireArguments().getBoolean(ARG_LYRICS_CHECKED)
        }
        popupMenu.menu.findItem(R.id.action_toggle_favorite)?.apply {
            isVisible = requireArguments().getBoolean(ARG_SHOW_FAVORITE)
            isChecked = requireArguments().getBoolean(ARG_FAVORITE_CHECKED)
            title = if (isChecked) {
                getString(R.string.action_remove_from_favorites)
            } else {
                getString(R.string.action_add_to_favorites)
            }
        }
        popupMenu.menu.findItem(R.id.now_playing)?.isVisible =
            requireArguments().getBoolean(ARG_SHOW_QUEUE)

        val entries = buildList {
            for (index in 0 until popupMenu.menu.size()) {
                val item = popupMenu.menu.getItem(index)
                if (item.isVisible) {
                    add(
                        Entry(
                            id = item.itemId,
                            title = item.title ?: "",
                            icon = item.icon?.constantState?.newDrawable()?.mutate(),
                            isCheckable = item.isCheckable,
                            isChecked = item.isChecked,
                        )
                    )
                }
            }
        }

        view.findViewById<RecyclerView>(R.id.playerMenuList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = MenuAdapter(entries) { entry ->
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(RESULT_ITEM_ID to entry.id),
                )
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(requireContext().surfaceColor()))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
        }
    }

    private data class Entry(
        val id: Int,
        val title: CharSequence,
        val icon: Drawable?,
        val isCheckable: Boolean,
        val isChecked: Boolean,
    )

    private class MenuAdapter(
        private val entries: List<Entry>,
        private val onClick: (Entry) -> Unit,
    ) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_player_menu, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(entries[position], onClick)
        }

        override fun getItemCount(): Int = entries.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.icon)
            private val title: TextView = itemView.findViewById(R.id.title)
            private val check: CheckBox = itemView.findViewById(R.id.check)

            fun bind(entry: Entry, onClick: (Entry) -> Unit) {
                title.text = entry.title
                icon.isVisible = entry.icon != null
                icon.setImageDrawable(entry.icon)
                icon.imageTintList = ColorStateList.valueOf(
                    MaterialColors.getColor(
                        itemView,
                        com.google.android.material.R.attr.colorOnSurface,
                    )
                )
                check.isVisible = entry.isCheckable
                check.isChecked = entry.isChecked
                itemView.setOnClickListener { onClick(entry) }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "player_menu_result"
        const val RESULT_ITEM_ID = "item_id"
        const val TAG = "PLAYER_MENU"

        private const val ARG_SHOW_LYRICS = "show_lyrics"
        private const val ARG_LYRICS_CHECKED = "lyrics_checked"
        private const val ARG_SHOW_FAVORITE = "show_favorite"
        private const val ARG_FAVORITE_CHECKED = "favorite_checked"
        private const val ARG_SHOW_QUEUE = "show_queue"

        fun newInstance(
            showLyrics: Boolean,
            lyricsChecked: Boolean,
            showFavorite: Boolean,
            favoriteChecked: Boolean,
            showQueue: Boolean,
        ) = PlayerMenuDialog().apply {
            arguments = bundleOf(
                ARG_SHOW_LYRICS to showLyrics,
                ARG_LYRICS_CHECKED to lyricsChecked,
                ARG_SHOW_FAVORITE to showFavorite,
                ARG_FAVORITE_CHECKED to favoriteChecked,
                ARG_SHOW_QUEUE to showQueue,
            )
        }
    }
}
