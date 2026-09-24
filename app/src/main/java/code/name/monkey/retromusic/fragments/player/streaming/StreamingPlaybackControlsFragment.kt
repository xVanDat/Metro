/*
 * Copyright (c) 2026 Metro contributors.
 * Licensed under the GNU General Public License v3.
 */
package code.name.monkey.retromusic.fragments.player.streaming

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.MainActivity
import code.name.monkey.retromusic.extensions.applyColor
import code.name.monkey.retromusic.fragments.base.AbsPlayerControlsFragment
import code.name.monkey.retromusic.fragments.base.goToAlbum
import code.name.monkey.retromusic.fragments.base.goToArtist
import code.name.monkey.retromusic.fragments.base.goToLyrics
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

abstract class StreamingPlaybackControlsFragment(@LayoutRes layout: Int) :
    AbsPlayerControlsFragment(layout) {

    private lateinit var titleView: TextView
    private lateinit var artistView: TextView
    private lateinit var currentTimeView: TextView
    private lateinit var totalTimeView: TextView
    private lateinit var sliderView: Slider
    private lateinit var shuffleView: ImageButton
    private lateinit var previousView: ImageButton
    private lateinit var playPauseView: ImageButton
    private lateinit var nextView: ImageButton
    private lateinit var repeatView: ImageButton
    private var favoriteView: ImageButton? = null
    private var queueAction: View? = null
    private var lyricsAction: View? = null

    var primaryColor: Int = Color.WHITE
        private set

    override val progressSlider: Slider get() = sliderView
    override val shuffleButton: ImageButton get() = shuffleView
    override val repeatButton: ImageButton get() = repeatView
    override val nextButton: ImageButton get() = nextView
    override val previousButton: ImageButton get() = previousView
    override val songTotalTime: TextView get() = totalTimeView
    override val songCurrentProgress: TextView get() = currentTimeView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleView = view.findViewById(R.id.title)
        artistView = view.findViewById(R.id.text)
        currentTimeView = view.findViewById(R.id.songCurrentProgress)
        totalTimeView = view.findViewById(R.id.songTotalTime)
        sliderView = view.findViewById(R.id.progressSlider)
        shuffleView = view.findViewById(R.id.shuffleButton)
        previousView = view.findViewById(R.id.previousButton)
        playPauseView = view.findViewById(R.id.playPauseButton)
        nextView = view.findViewById(R.id.nextButton)
        repeatView = view.findViewById(R.id.repeatButton)
        favoriteView = view.findViewById(R.id.favoriteButton)
        queueAction = view.findViewById(R.id.queueAction)
        lyricsAction = view.findViewById(R.id.lyricsAction)

        titleView.isSelected = true
        artistView.isSelected = true
        titleView.setOnClickListener { goToAlbum(requireActivity()) }
        artistView.setOnClickListener { goToArtist(requireActivity()) }
        playPauseView.setOnClickListener {
            if (MusicPlayerRemote.isPlaying) MusicPlayerRemote.pauseSong()
            else MusicPlayerRemote.resumePlaying()
            it.showBounceAnimation()
        }
        favoriteView?.setOnClickListener {
            (parentFragment as? StreamingPlayerFragment)?.onFavoriteToggled()
        }
        lyricsAction?.setOnClickListener { goToLyrics(requireActivity()) }
        queueAction?.setOnClickListener {
            (requireActivity() as? MainActivity)?.apply {
                findNavController(R.id.fragment_container).navigate(
                    R.id.playing_queue_fragment,
                    null,
                    navOptions { launchSingleTop = true },
                )
                collapsePanel()
            }
        }
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        titleView.text = song.title
        artistView.text = song.artistName
        refreshFavorite()
    }

    fun refreshFavorite() {
        if (!isAdded || favoriteView == null) return
        lifecycleScope.launch {
            val isFavorite = MusicUtil.isFavorite(MusicPlayerRemote.currentSong)
            favoriteView?.setImageResource(
                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
            )
        }
    }

    override fun onServiceConnected() {
        updateSong()
        updatePlayPauseDrawableState()
        updateRepeatState()
        updateShuffleState()
    }

    override fun onPlayingMetaChanged() {
        updateSong()
    }

    override fun onFavoriteStateChanged() = refreshFavorite()

    override fun onPlayStateChanged() = updatePlayPauseDrawableState()

    override fun onRepeatModeChanged() = updateRepeatState()

    override fun onShuffleModeChanged() = updateShuffleState()

    override fun setColor(color: MediaNotificationProcessor) {
        primaryColor = color.primaryTextColor
        lastPlaybackControlsColor = color.primaryTextColor
        lastDisabledPlaybackControlsColor = ColorUtil.withAlpha(color.primaryTextColor, 0.35f)

        titleView.setTextColor(color.primaryTextColor)
        artistView.setTextColor(color.secondaryTextColor)
        currentTimeView.setTextColor(color.secondaryTextColor)
        totalTimeView.setTextColor(color.secondaryTextColor)
        progressSlider.applyColor(color.primaryTextColor)

        TintHelper.setTintAuto(playPauseView, Color.BLACK, false)
        TintHelper.setTintAuto(playPauseView, Color.WHITE, true)
        favoriteView?.setColorFilter(color.primaryTextColor)
        tintAction(queueAction, color.primaryTextColor)
        tintAction(lyricsAction, color.primaryTextColor)
        updatePrevNextColor()
        updateRepeatState()
        updateShuffleState()
    }

    private fun tintAction(view: View?, color: Int) {
        when (view) {
            is ImageButton -> view.setColorFilter(color)
            is TextView -> view.setTextColor(color)
        }
    }

    private fun updatePlayPauseDrawableState() {
        playPauseView.setImageResource(
            if (MusicPlayerRemote.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
        )
    }

    public override fun show() {
        playPauseView.animate().scaleX(1f).scaleY(1f).rotation(360f)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    public override fun hide() {
        playPauseView.scaleX = 0f
        playPauseView.scaleY = 0f
        playPauseView.rotation = 0f
    }
}
