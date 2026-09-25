package code.name.monkey.retromusic.providers

import android.content.Context
import android.content.Intent
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.MusicService

/** Stores songs hidden individually, independently from the folder blacklist. */
class HiddenSongsStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val songIds: Set<Long>
        get() = preferences.getStringSet(KEY_IDS, emptySet()).orEmpty()
            .mapNotNullTo(linkedSetOf()) { it.toLongOrNull() }

    val entries: List<HiddenSong>
        get() = songIds.map { id ->
            HiddenSong(
                id,
                preferences.getString(titleKey(id), null).orEmpty(),
                preferences.getString(artistKey(id), null).orEmpty()
            )
        }.sortedBy { it.title.lowercase() }

    fun hide(song: Song): Boolean {
        val ids = songIds.toMutableSet()
        if (!ids.add(song.id)) return false
        preferences.edit()
            .putStringSet(KEY_IDS, ids.mapTo(linkedSetOf()) { it.toString() })
            .putString(titleKey(song.id), song.title)
            .putString(artistKey(song.id), song.artistName)
            .apply()
        notifyLibraryChanged()
        return true
    }

    fun restore(songId: Long) {
        val ids = songIds.toMutableSet().apply { remove(songId) }
        preferences.edit()
            .putStringSet(KEY_IDS, ids.mapTo(linkedSetOf()) { it.toString() })
            .remove(titleKey(songId))
            .remove(artistKey(songId))
            .apply()
        notifyLibraryChanged()
    }

    fun clear() {
        preferences.edit().clear().apply()
        notifyLibraryChanged()
    }

    private fun notifyLibraryChanged() {
        appContext.sendBroadcast(Intent(MusicService.MEDIA_STORE_CHANGED))
    }

    data class HiddenSong(val id: Long, val title: String, val artist: String) {
        val displayName: String
            get() = if (artist.isBlank()) title else "$title\n$artist"
    }

    companion object {
        private const val PREFERENCES_NAME = "hidden_songs"
        private const val KEY_IDS = "song_ids"
        private fun titleKey(id: Long) = "title_$id"
        private fun artistKey(id: Long) = "artist_$id"

        @Volatile
        private var instance: HiddenSongsStore? = null

        fun getInstance(context: Context): HiddenSongsStore = instance ?: synchronized(this) {
            instance ?: HiddenSongsStore(context).also { instance = it }
        }
    }
}
