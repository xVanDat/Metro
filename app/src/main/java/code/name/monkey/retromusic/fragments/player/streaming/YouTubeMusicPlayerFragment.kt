/* Copyright (c) 2026 Metro contributors. Licensed under GPLv3. */
package code.name.monkey.retromusic.fragments.player.streaming

import code.name.monkey.retromusic.R

class YouTubeMusicPlayerFragment : StreamingPlayerFragment(
    R.layout.fragment_youtube_music_player,
    gradientBackground = false,
)

class YouTubeMusicPlaybackControlsFragment : StreamingPlaybackControlsFragment(
    R.layout.fragment_youtube_music_playback_controls,
)
