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
package code.name.monkey.retromusic.fragments

import androidx.annotation.StringRes
import code.name.monkey.retromusic.R

enum class NowPlayingScreen constructor(
    @param:StringRes @field:StringRes
    val titleRes: Int,
    val id: Int,
    val defaultCoverTheme: AlbumCoverStyle?
) {
    // Some Now playing themes look better with particular Album cover theme

    Adaptive(R.string.adaptive, 10, AlbumCoverStyle.FullCard),
    AnimatedGradient(R.string.animated_gradient, 19, AlbumCoverStyle.Normal),
    Blur(R.string.blur, 4, AlbumCoverStyle.Normal),
    BlurCard(R.string.blur_card, 9, AlbumCoverStyle.Card),
    Card(R.string.card, 6, AlbumCoverStyle.Full),
    Circle(R.string.circle, 15, null),
    Classic(R.string.classic, 16, AlbumCoverStyle.Full),
    Color(R.string.color, 5, AlbumCoverStyle.Normal),
    Fit(R.string.fit, 12, AlbumCoverStyle.Full),
    Flat(R.string.flat, 1, AlbumCoverStyle.Flat),
    Full(R.string.full, 2, AlbumCoverStyle.Full),
    Gradient(R.string.gradient, 17, AlbumCoverStyle.Full),
    Material(R.string.material, 11, AlbumCoverStyle.Normal),
    MD3(R.string.md3, 18, AlbumCoverStyle.Normal),
    Normal(R.string.normal, 0, AlbumCoverStyle.Normal),
    Peek(R.string.peek, 14, AlbumCoverStyle.Normal),
    Plain(R.string.plain, 3, AlbumCoverStyle.Normal),
    Simple(R.string.simple, 8, AlbumCoverStyle.Normal),
    Spotify(R.string.spotify_style, 21, AlbumCoverStyle.Normal),
    Tiny(R.string.tiny, 7, null),
    YouTubeMusic(R.string.youtube_music_style, 20, AlbumCoverStyle.Normal),
}
