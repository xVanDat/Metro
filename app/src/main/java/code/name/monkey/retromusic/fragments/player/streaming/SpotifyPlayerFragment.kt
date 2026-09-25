/*
 * Copyright (c) 2026 Metro contributors.
 * Licensed under the GNU General Public License v3.
 */
package code.name.monkey.retromusic.fragments.player.streaming

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.SHOW_LYRICS
import code.name.monkey.retromusic.fragments.base.goToLyrics
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.model.lyrics.AbsSynchronizedLyrics
import code.name.monkey.retromusic.model.lyrics.Lyrics
import code.name.monkey.retromusic.util.LyricUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.exceptions.CannotReadException
import java.io.FileNotFoundException

class SpotifyPlayerFragment : StreamingPlayerFragment(
    R.layout.fragment_spotify_player,
    gradientBackground = true,
    supportsSyncedLyrics = true,
), MusicProgressViewUpdateHelper.Callback, SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var progressHelper: MusicProgressViewUpdateHelper
    private var lyricsPreview: TextView? = null
    private var lyrics: Lyrics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressHelper = MusicProgressViewUpdateHelper(this, 500, 1000)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lyricsPreview = view.findViewById<TextView>(R.id.lyricsPreview).apply {
            setOnClickListener { goToLyrics(requireActivity()) }
        }
        updateLyrics()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateLyrics()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateLyrics()
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)
        if (PreferenceUtil.showLyrics) {
            progressHelper.start()
            updateLyrics()
        } else {
            hideLyrics()
        }
    }

    override fun onPause() {
        progressHelper.stop()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onDestroyView() {
        lyricsPreview = null
        lyrics = null
        super.onDestroyView()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        val synchronizedLyrics = lyrics as? AbsSynchronizedLyrics
        if (!PreferenceUtil.showLyrics || synchronizedLyrics?.isValid != true) {
            hideLyrics()
            return
        }

        val line = synchronizedLyrics.getLine(progress).trim()
        lyricsPreview?.apply {
            isVisible = line.isNotEmpty()
            if (text.toString() != line) text = line
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != SHOW_LYRICS) return
        if (PreferenceUtil.showLyrics) {
            progressHelper.start()
            updateLyrics()
        } else {
            progressHelper.stop()
            lyrics = null
            hideLyrics()
        }
    }

    private fun updateLyrics() {
        val song = MusicPlayerRemote.currentSong
        lyrics = null
        hideLyrics()
        if (song.id < 0 || !PreferenceUtil.showLyrics) return

        lifecycleScope.launch(Dispatchers.IO) {
            val parsed = try {
                val lrcFile = LyricUtil.getSyncedLyricsFile(song)
                val data = LyricUtil.getStringFromLrc(lrcFile)
                Lyrics.parse(
                    song,
                    data.ifEmpty { LyricUtil.getEmbeddedSyncedLyrics(song.data) },
                )
            } catch (error: FileNotFoundException) {
                null
            } catch (error: CannotReadException) {
                null
            }

            withContext(Dispatchers.Main) {
                if (!isAdded || MusicPlayerRemote.currentSong.id != song.id) return@withContext
                lyrics = parsed
                onUpdateProgressViews(
                    MusicPlayerRemote.songProgressMillis,
                    MusicPlayerRemote.songDurationMillis,
                )
            }
        }
    }

    private fun hideLyrics() {
        lyricsPreview?.apply {
            isVisible = false
            text = null
        }
    }
}

class SpotifyPlaybackControlsFragment : StreamingPlaybackControlsFragment(
    R.layout.fragment_spotify_playback_controls,
    forceLightControls = true,
)
