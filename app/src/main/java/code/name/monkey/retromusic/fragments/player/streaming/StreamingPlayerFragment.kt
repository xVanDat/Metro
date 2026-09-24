/*
 * Copyright (c) 2026 Metro contributors.
 * Licensed under the GNU General Public License v3.
 */
package code.name.monkey.retromusic.fragments.player.streaming

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.drawAboveSystemBars
import code.name.monkey.retromusic.fragments.base.AbsPlayerFragment
import code.name.monkey.retromusic.fragments.player.PlayerAlbumCoverFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor

abstract class StreamingPlayerFragment(
    @LayoutRes layout: Int,
    private val gradientBackground: Boolean,
) : AbsPlayerFragment(layout) {

    private var lastColor = Color.BLACK
    private lateinit var controlsFragment: StreamingPlaybackControlsFragment
    private var toolbar: Toolbar? = null
    private var backgroundView: View? = null

    override val paletteColor: Int
        get() = lastColor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.playerToolbar)
        backgroundView = view.findViewById(R.id.colorGradientBackground)
        controlsFragment = childFragmentManager.findFragmentById(R.id.playbackControlsFragment)
            as StreamingPlaybackControlsFragment
        (childFragmentManager.findFragmentById(R.id.playerAlbumCoverFragment) as PlayerAlbumCoverFragment)
            .setCallbacks(this)
        setUpToolbar()
        playerToolbar()?.drawAboveSystemBars()
    }

    private fun setUpToolbar() {
        toolbar?.apply {
            inflateMenu(R.menu.menu_player)
            menu.findItem(R.id.action_toggle_lyrics)?.isVisible = false
            menu.findItem(R.id.now_playing)?.isVisible = false
            for (index in 0 until menu.size()) {
                menu.getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            setOnMenuItemClickListener(this@StreamingPlayerFragment)
        }
    }

    override fun onColorChanged(color: MediaNotificationProcessor) {
        lastColor = color.backgroundColor
        libraryViewModel.updateColor(color.backgroundColor)
        controlsFragment.setColor(color)
        backgroundView?.background = if (gradientBackground) {
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    color.backgroundColor,
                    ColorUtils.blendARGB(color.backgroundColor, Color.BLACK, 0.48f),
                    ColorUtils.blendARGB(color.backgroundColor, Color.BLACK, 0.82f),
                ),
            )
        } else {
            ColorDrawable(ColorUtils.blendARGB(color.backgroundColor, Color.BLACK, 0.28f))
        }
        toolbar?.let {
            ToolbarContentTintHelper.colorizeToolbar(
                it,
                color.primaryTextColor,
                requireActivity(),
            )
        }
    }

    override fun onShow() = controlsFragment.show()

    override fun onHide() = controlsFragment.hide()

    override fun toolbarIconColor(): Int = controlsFragment.primaryColor

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        controlsFragment.refreshFavorite()
    }

    override fun onDestroyView() {
        toolbar = null
        backgroundView = null
        super.onDestroyView()
    }

    override fun playerToolbar(): Toolbar? = toolbar
}
