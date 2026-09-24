package code.name.monkey.retromusic.util

import code.name.monkey.retromusic.model.Song
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.TagTextField
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

object ReplayGainUtil {
    const val MODE_OFF = "off"
    const val MODE_TRACK = "track"
    const val MODE_ALBUM = "album"

    private const val TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
    private const val ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"
    private val gainPattern = Regex("[-+]?\\d+(?:[.,]\\d+)?")
    private val cache = ConcurrentHashMap<String, Float>()

    fun multiplier(song: Song, mode: String = PreferenceUtil.replayGainMode): Float {
        if (mode == MODE_OFF || song.data.isBlank()) return 1f
        val file = File(song.data)
        if (!file.isFile) return 1f
        val cacheKey = "${file.absolutePath}:${file.lastModified()}:$mode"
        return cache.getOrPut(cacheKey) {
            runCatching {
                val tag = AudioFileIO.read(file).tag ?: return@runCatching 1f
                val preferredKey = if (mode == MODE_ALBUM) ALBUM_GAIN else TRACK_GAIN
                val value = findTagValue(tag.fields, preferredKey)
                    ?: (if (mode == MODE_ALBUM) findTagValue(tag.fields, TRACK_GAIN) else null)
                    ?: return@runCatching 1f
                val decibels = gainPattern.find(value)?.value
                    ?.replace(',', '.')
                    ?.toFloatOrNull()
                    ?: return@runCatching 1f
                // MediaPlayer volume is limited to 1.0, so apply attenuation safely and avoid clipping.
                10.0.pow(decibels.coerceIn(-24f, 0f) / 20.0).toFloat()
            }.getOrDefault(1f)
        }
    }

    private fun findTagValue(fields: Iterator<org.jaudiotagger.tag.TagField>, key: String): String? {
        fields.forEach { field ->
            if (field is AbstractID3v2Frame) {
                val body = field.body
                if (body is FrameBodyTXXX && body.description.equals(key, ignoreCase = true)) {
                    return body.text
                }
            }
            if (field.id.equals(key, ignoreCase = true) && field is TagTextField) {
                return field.content
            }
        }
        return null
    }
}
