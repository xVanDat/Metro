package code.name.monkey.retromusic.providers

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import code.name.monkey.retromusic.BOOKMARKS
import code.name.monkey.retromusic.RESUME_POSITIONS
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

object PlaybackPositionStore {
    private const val MIN_TRACK_DURATION = 10 * 60 * 1000L
    private const val MIN_RESUME_POSITION = 30 * 1000
    private const val MIN_REMAINING_DURATION = 30 * 1000L
    private const val MAX_RESUME_ENTRIES = 200

    private val gson = Gson()
    private val resumeType = object : TypeToken<LinkedHashMap<String, Long>>() {}.type
    private val bookmarkType = object : TypeToken<List<PlaybackBookmark>>() {}.type

    @Synchronized
    fun saveResumePosition(context: Context, song: Song, position: Int) {
        if (!PreferenceUtil.isResumeLongTracks || song.id < 0 || song.duration < MIN_TRACK_DURATION) {
            return
        }
        val positions = readResumePositions(context)
        if (position < MIN_RESUME_POSITION || song.duration - position < MIN_REMAINING_DURATION) {
            positions.remove(song.data)
        } else {
            positions.remove(song.data)
            positions[song.data] = position.toLong()
            while (positions.size > MAX_RESUME_ENTRIES) {
                positions.remove(positions.keys.first())
            }
        }
        writeResumePositions(context, positions)
    }

    @Synchronized
    fun resumePosition(context: Context, song: Song): Int {
        if (!PreferenceUtil.isResumeLongTracks || song.duration < MIN_TRACK_DURATION) return -1
        return readResumePositions(context)[song.data]?.toInt() ?: -1
    }

    @Synchronized
    fun clearResumePosition(context: Context, song: Song) {
        val positions = readResumePositions(context)
        if (positions.remove(song.data) != null) {
            writeResumePositions(context, positions)
        }
    }

    @Synchronized
    fun addBookmark(context: Context, song: Song, position: Int): Boolean {
        if (song.id < 0 || position <= 0) return false
        val bookmarks = readBookmarks(context).toMutableList()
        if (bookmarks.any { it.songPath == song.data && kotlin.math.abs(it.position - position) < 1000 }) {
            return false
        }
        bookmarks.add(PlaybackBookmark(song.data, position, System.currentTimeMillis()))
        writeBookmarks(context, bookmarks.sortedWith(compareBy({ it.songPath }, { it.position })))
        return true
    }

    @Synchronized
    fun bookmarksFor(context: Context, song: Song): List<PlaybackBookmark> {
        return readBookmarks(context).filter { it.songPath == song.data }.sortedBy { it.position }
    }

    @Synchronized
    fun clearBookmarks(context: Context, song: Song) {
        writeBookmarks(context, readBookmarks(context).filterNot { it.songPath == song.data })
    }

    private fun readResumePositions(context: Context): LinkedHashMap<String, Long> {
        val json = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(RESUME_POSITIONS, null) ?: return linkedMapOf()
        return try {
            gson.fromJson<LinkedHashMap<String, Long>>(json, resumeType) ?: linkedMapOf()
        } catch (_: JsonSyntaxException) {
            linkedMapOf()
        }
    }

    private fun writeResumePositions(context: Context, positions: LinkedHashMap<String, Long>) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(RESUME_POSITIONS, gson.toJson(positions, resumeType))
        }
    }

    private fun readBookmarks(context: Context): List<PlaybackBookmark> {
        val json = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(BOOKMARKS, null) ?: return emptyList()
        return try {
            gson.fromJson<List<PlaybackBookmark>>(json, bookmarkType) ?: emptyList()
        } catch (_: JsonSyntaxException) {
            emptyList()
        }
    }

    private fun writeBookmarks(context: Context, bookmarks: List<PlaybackBookmark>) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(BOOKMARKS, gson.toJson(bookmarks, bookmarkType))
        }
    }
}

data class PlaybackBookmark(
    val songPath: String,
    val position: Int,
    val createdAt: Long
)
