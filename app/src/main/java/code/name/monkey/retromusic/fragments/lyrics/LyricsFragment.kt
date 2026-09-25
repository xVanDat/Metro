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
package code.name.monkey.retromusic.fragments.lyrics

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.transition.Fade
import code.name.monkey.appthemehelper.common.ATHToolbarActivity
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.tageditor.TagWriter
import code.name.monkey.retromusic.databinding.FragmentLyricsBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.setLightNavigationBarAuto
import code.name.monkey.retromusic.extensions.setLightStatusBarAuto
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.extensions.openUrl
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.glide.palette.BitmapPaletteWrapper
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.lyrics.LrcView
import code.name.monkey.retromusic.model.AudioTagInfo
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.FileUtils
import code.name.monkey.retromusic.util.LyricUtil
import code.name.monkey.retromusic.util.UriUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.afollestad.materialdialogs.input.input
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.set

class LyricsFragment : AbsMainActivityFragment(R.layout.fragment_lyrics),
    MusicProgressViewUpdateHelper.Callback {

    private var _binding: FragmentLyricsBinding? = null
    private val binding get() = _binding!!
    private lateinit var song: Song

    private lateinit var normalLyricsLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var editSyncedLyricsLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var cacheFile: File
    private var syncedLyrics: String = ""
    private lateinit var syncedFileUri: Uri

    private var lyricsType: LyricsType = LyricsType.NORMAL_LYRICS

    private val googleSearchLrcUrl: String
        get() {
            var baseUrl = "http://www.google.com/search?"
            var query = song.title + "+" + song.artistName
            query = "q=" + query.replace(" ", "+") + " lyrics"
            baseUrl += query
            return baseUrl
        }

    private lateinit var updateHelper: MusicProgressViewUpdateHelper
    private var paletteTarget: Target<BitmapPaletteWrapper>? = null
    private var previousStatusBarColor: Int? = null
    private var previousNavigationBarColor: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Normal lyrics launcher
        normalLyricsLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    FileUtils.copyFileToUri(requireContext(), cacheFile, song.uri)
                }
            }
        editSyncedLyricsLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    requireContext().contentResolver.openOutputStream(syncedFileUri)?.use { os ->
                        (os as FileOutputStream).channel.truncate(0)
                        os.write(syncedLyrics.toByteArray())
                        os.flush()
                    }
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterTransition = Fade()
        exitTransition = Fade()
        _binding = FragmentLyricsBinding.bind(view)
        previousStatusBarColor = requireActivity().window.statusBarColor
        previousNavigationBarColor = requireActivity().window.navigationBarColor
        updateHelper = MusicProgressViewUpdateHelper(this, 500, 1000)
        setupLyricsView()
        updateTitleSong()
        loadLyrics()

        setupWakelock()
        setupViews()
        setupToolbar()
    }

    private fun setupLyricsView() {
        binding.lyricsView.apply {
            setCurrentColor(accentColor())
            setTimeTextColor(accentColor())
            setTimelineColor(accentColor())
            setTimelineTextColor(accentColor())
            setDraggable(true, LrcView.OnPlayClickListener {
                MusicPlayerRemote.seekTo(it.toInt())
                return@OnPlayClickListener true
            })
        }
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.lyricsView.updateTime(progress.toLong())
    }

    private fun setupViews() {
        binding.editButton.accentColor()
        binding.editButton.setOnClickListener {
            when (lyricsType) {
                LyricsType.SYNCED_LYRICS -> {
                    editSyncedLyrics()
                }
                LyricsType.NORMAL_LYRICS -> {
                    editNormalLyrics()
                }
            }
        }
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateTitleSong()
        loadLyrics()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateTitleSong()
        loadLyrics()
    }

    private fun updateTitleSong() {
        song = MusicPlayerRemote.currentSong
        updateLyricsBackground()
    }

    private fun updateLyricsBackground() {
        paletteTarget?.let { Glide.with(this).clear(it) }
        val currentSong = song
        if (currentSong.id < 0) {
            applyLyricsColors(MediaNotificationProcessor.errorColor(requireContext()))
            return
        }
        paletteTarget = Glide.with(this)
            .asBitmapPalette()
            .songCoverOptions(currentSong)
            .load(RetroGlideExtension.getSongModel(currentSong))
            .into(object : CustomTarget<BitmapPaletteWrapper>() {
                override fun onResourceReady(
                    resource: BitmapPaletteWrapper,
                    transition: Transition<in BitmapPaletteWrapper>?,
                ) {
                    if (!isAdded || MusicPlayerRemote.currentSong.id != currentSong.id) return
                    applyLyricsColors(
                        MediaNotificationProcessor(requireContext(), resource.bitmap)
                    )
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    if (isAdded) {
                        applyLyricsColors(MediaNotificationProcessor.errorColor(requireContext()))
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit
            })
    }

    private fun applyLyricsColors(colors: MediaNotificationProcessor) {
        val background = colors.backgroundColor
        val primary = colors.primaryTextColor
        val secondary = colors.secondaryTextColor

        binding.container.setBackgroundColor(background)
        binding.appBarLayout.setBackgroundColor(background)
        binding.toolbar.setBackgroundColor(background)
        ToolbarContentTintHelper.colorizeToolbar(
            binding.toolbar,
            primary,
            requireActivity(),
        )
        binding.normalLyrics.setTextColor(primary)
        binding.noLyricsFound.setTextColor(primary)
        binding.lyricsView.apply {
            setCurrentColor(primary)
            setNormalColor(secondary)
            setTimeTextColor(primary)
            setTimelineColor(primary)
            setTimelineTextColor(primary)
        }
        binding.editButton.backgroundTintList = ColorStateList.valueOf(primary)
        binding.editButton.imageTintList = ColorStateList.valueOf(background)

        requireActivity().window.apply {
            statusBarColor = background
            navigationBarColor = background
        }
        mainActivity.setLightStatusBarAuto(background)
        mainActivity.setLightNavigationBarAuto(background)
    }

    private fun setupToolbar() {
        mainActivity.setSupportActionBar(binding.toolbar)
        ToolbarContentTintHelper.colorBackButton(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupWakelock() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_lyrics, menu)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            binding.toolbar,
            menu,
            ATHToolbarActivity.getToolbarBackgroundColor(binding.toolbar)
        )
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_search) {
            openUrl(googleSearchLrcUrl)
        }
        return false
    }

    @SuppressLint("CheckResult")
    private fun editNormalLyrics(lyrics: String? = null) {
        val file = File(song.data)
        val content = lyrics ?: try {
            AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        val song = song

        materialDialog().show {
            title(res = R.string.edit_normal_lyrics)
            input(
                hintRes = R.string.paste_lyrics_here,
                prefill = content,
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            ) { _, input ->
                val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
                fieldKeyValueMap[FieldKey.LYRICS] = input.toString()
                GlobalScope.launch {
                    if (VersionUtils.hasR()) {
                        cacheFile = TagWriter.writeTagsToFilesR(
                            requireContext(), AudioTagInfo(
                                listOf(song.data), fieldKeyValueMap, null
                            )
                        )[0]
                        val pendingIntent =
                            MediaStore.createWriteRequest(
                                requireContext().contentResolver,
                                listOf(song.uri)
                            )

                        normalLyricsLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent).build()
                        )
                    } else {
                        TagWriter.writeTagsToFiles(
                            requireContext(), AudioTagInfo(
                                listOf(song.data), fieldKeyValueMap, null
                            )
                        )
                    }
                }
            }
            positiveButton(res = R.string.save) {
                loadNormalLyrics()
            }
            negativeButton(res = android.R.string.cancel)
        }
    }


    @SuppressLint("CheckResult")
    private fun editSyncedLyrics(lyrics: String? = null) {
        val content = lyrics ?: LyricUtil.getStringFromLrc(LyricUtil.getSyncedLyricsFile(song))

        val song = song
        materialDialog().show {
            title(res = R.string.edit_synced_lyrics)
            input(
                hintRes = R.string.paste_timeframe_lyrics_here,
                prefill = content,
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            ) { _, input ->
                if (VersionUtils.hasR()) {
                    syncedLyrics = input.toString()
                    val lrcFile = LyricUtil.getSyncedLyricsFile(song)
                    if (lrcFile?.exists() == true) {
                        syncedFileUri =
                            UriUtil.getUriFromPath(requireContext(), lrcFile.absolutePath)
                        val pendingIntent =
                            MediaStore.createWriteRequest(
                                requireContext().contentResolver,
                                listOf(syncedFileUri)
                            )
                        editSyncedLyricsLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent).build()
                        )
                    } else {
                        val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
                        fieldKeyValueMap[FieldKey.LYRICS] = input.toString()
                        GlobalScope.launch {
                            cacheFile = TagWriter.writeTagsToFilesR(
                                requireContext(),
                                AudioTagInfo(listOf(song.data), fieldKeyValueMap, null)
                            )[0]
                            val pendingIntent = MediaStore.createWriteRequest(
                                requireContext().contentResolver,
                                listOf(song.uri)
                            )

                            normalLyricsLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent).build()
                            )
                        }
                    }
                } else {
                    LyricUtil.writeLrc(song, input.toString())
                }
            }
            positiveButton(res = R.string.save) {
                loadLRCLyrics()
            }
            negativeButton(res = android.R.string.cancel)
        }
    }

    private fun loadNormalLyrics() {
        val file = File(song.data)
        val lyrics = try {
            AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
        binding.normalLyrics.isVisible = !lyrics.isNullOrEmpty()
        binding.noLyricsFound.isVisible = lyrics.isNullOrEmpty()
        binding.normalLyrics.text = lyrics
    }

    /**
     * @return success
     */
    private fun loadLRCLyrics(): Boolean {
        val lrcFile = LyricUtil.getSyncedLyricsFile(song)
        if (lrcFile != null) {
            binding.lyricsView.loadLrc(lrcFile)
        } else {
            val embeddedLyrics = LyricUtil.getEmbeddedSyncedLyrics(song.data)
            if (embeddedLyrics != null) {
                binding.lyricsView.loadLrc(embeddedLyrics)
            } else {
                binding.lyricsView.setLabel(getString(R.string.empty))
                return false
            }
        }
        return true
    }

    private fun loadLyrics() {
        lyricsType = if (!loadLRCLyrics()) {
            binding.lyricsView.isVisible = false
            loadNormalLyrics()
            LyricsType.NORMAL_LYRICS
        } else {
            binding.normalLyrics.isVisible = false
            binding.noLyricsFound.isVisible = false
            binding.lyricsView.isVisible = true
            LyricsType.SYNCED_LYRICS
        }
    }

    override fun onResume() {
        super.onResume()
        updateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        updateHelper.stop()
    }

    override fun onDestroyView() {
        paletteTarget?.let { Glide.with(this).clear(it) }
        paletteTarget = null
        previousStatusBarColor?.let {
            requireActivity().window.statusBarColor = it
            mainActivity.setLightStatusBarAuto(it)
        }
        previousNavigationBarColor?.let {
            requireActivity().window.navigationBarColor = it
            mainActivity.setLightNavigationBarAuto(it)
        }
        if (MusicPlayerRemote.playingQueue.isNotEmpty())
            mainActivity.expandPanel()
        _binding = null
        super.onDestroyView()
    }

    enum class LyricsType {
        NORMAL_LYRICS,
        SYNCED_LYRICS
    }
}
