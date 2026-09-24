/*
 * Copyright (c) 2026 Metro contributors.
 * Licensed under the GNU General Public License v3.
 */
package code.name.monkey.retromusic.fragments.player.streaming

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.fragments.base.goToLyrics
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.util.MusicUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpotifyPlayerFragment : StreamingPlayerFragment(
    R.layout.fragment_spotify_player,
    gradientBackground = true,
), MusicProgressViewUpdateHelper.Callback {

    private data class LyricLine(val timeMs: Long?, val text: String)

    private lateinit var progressHelper: MusicProgressViewUpdateHelper
    private var lyricsPreview: TextView? = null
    private var lyricLines: List<LyricLine> = emptyList()
    private var displayedLine = -1
    private var lyricsSongId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressHelper = MusicProgressViewUpdateHelper(this, 300, 600)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lyricsPreview = view.findViewById<TextView>(R.id.lyricsPreview).apply {
            setOnClickListener { goToLyrics(requireActivity()) }
        }
        loadLyrics()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        loadLyrics()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        loadLyrics()
    }

    override fun onResume() {
        super.onResume()
        progressHelper.start()
    }

    override fun onPause() {
        progressHelper.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        lyricsPreview = null
        lyricLines = emptyList()
        super.onDestroyView()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        if (lyricLines.isEmpty()) return
        val timed = lyricLines.firstOrNull()?.timeMs != null
        val index = if (timed) {
            lyricLines.indexOfLast { (it.timeMs ?: Long.MAX_VALUE) <= progress }.coerceAtLeast(0)
        } else {
            if (total <= 0) 0 else ((progress.toFloat() / total) * lyricLines.size)
                .toInt().coerceIn(0, lyricLines.lastIndex)
        }
        if (index != displayedLine) {
            displayedLine = index
            lyricsPreview?.text = lyricLines[index].text
        }
    }

    private fun loadLyrics() {
        val song = MusicPlayerRemote.currentSong
        if (song.id < 0 || song.id == lyricsSongId) return
        lyricsSongId = song.id
        displayedLine = -1
        lyricsPreview?.isVisible = false
        lifecycleScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                parseLyrics(MusicUtil.getLyrics(song))
            }
            if (!isAdded || MusicPlayerRemote.currentSong.id != song.id) return@launch
            lyricLines = parsed
            lyricsPreview?.isVisible = parsed.isNotEmpty()
            if (parsed.isNotEmpty()) {
                onUpdateProgressViews(
                    MusicPlayerRemote.songProgressMillis,
                    MusicPlayerRemote.songDurationMillis,
                )
            }
        }
    }

    private fun parseLyrics(rawLyrics: String?): List<LyricLine> {
        val raw = rawLyrics?.trim().orEmpty()
        if (raw.isBlank() || raw.equals("No lyrics found", ignoreCase = true)) return emptyList()

        val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.*)""")
        val timedLines = raw.lineSequence().mapNotNull { line ->
            val match = timestamp.find(line) ?: return@mapNotNull null
            val text = match.groupValues[4].trim()
            if (text.isEmpty()) return@mapNotNull null
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            LyricLine((minutes * 60_000L) + (seconds * 1_000L) + fraction, text)
        }.sortedBy { it.timeMs }.toList()
        if (timedLines.isNotEmpty()) return timedLines

        return raw.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.matches(Regex("""\[[a-zA-Z]+:.*]""")) }
            .map { LyricLine(null, it) }
            .toList()
    }
}

class SpotifyPlaybackControlsFragment : StreamingPlaybackControlsFragment(
    R.layout.fragment_spotify_playback_controls,
)
