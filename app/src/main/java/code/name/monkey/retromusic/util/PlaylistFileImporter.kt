package code.name.monkey.retromusic.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import code.name.monkey.retromusic.model.Song
import java.io.File

object PlaylistFileImporter {
    data class Result(val name: String, val entryCount: Int, val songs: List<Song>)

    fun parse(context: Context, uri: Uri, librarySongs: List<Song>): Result {
        val displayName = queryDisplayName(context, uri) ?: "Imported playlist.m3u"
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText().removePrefix("\uFEFF") }
            ?: error("Unable to open playlist")
        val entries = when (extension) {
            "pls" -> parsePls(text)
            "xspf" -> parseXmlLocations(text)
            "wpl" -> parseXmlAttribute(text, "media", "src")
            "asx" -> parseXmlAttribute(text, "ref", "href")
            else -> parseM3u(text)
        }.map(::cleanEntry).filter { it.isNotBlank() }

        val playlistFile = uri.toLocalStorageDocumentFile(context)
        val byPath = librarySongs.associateBy { normalizePath(it.data) }
        val byFileName = librarySongs.groupBy { File(it.data).name.lowercase() }
        val matched = LinkedHashMap<Long, Song>()
        entries.forEach { entry ->
            val absolutePath = resolvePath(entry, playlistFile?.parentFile)
            val song = byPath[normalizePath(absolutePath)] ?: run {
                val candidates = byFileName[File(absolutePath).name.lowercase()].orEmpty()
                candidates.singleOrNull()
            }
            if (song != null) matched.putIfAbsent(song.id, song)
        }

        val playlistName = displayName.substringBeforeLast('.').ifBlank { displayName }
        return Result(playlistName, entries.size, matched.values.toList())
    }

    private fun parseM3u(text: String) = text.lineSequence()
        .map(String::trim)
        .filter { it.isNotBlank() && !it.startsWith('#') }
        .toList()

    private fun parsePls(text: String): List<String> {
        val fileLine = Regex("(?im)^File(\\d+)=(.+)$")
        return fileLine.findAll(text)
            .map { it.groupValues[1].toInt() to it.groupValues[2].trim() }
            .sortedBy { it.first }
            .map { it.second }
            .toList()
    }

    private fun parseXmlLocations(text: String): List<String> =
        Regex("(?is)<location[^>]*>(.*?)</location>").findAll(text)
            .map { decodeXml(it.groupValues[1]) }.toList()

    private fun parseXmlAttribute(text: String, element: String, attribute: String): List<String> =
        Regex("(?is)<$element[^>]*\\s$attribute\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>")
            .findAll(text).map { decodeXml(it.groupValues[1]) }.toList()

    private fun cleanEntry(value: String): String {
        val decoded = Uri.decode(decodeXml(value.trim().trim('"', '\'')))
        return if (decoded.startsWith("file://", ignoreCase = true)) {
            Uri.parse(decoded).path.orEmpty()
        } else decoded
    }

    private fun resolvePath(entry: String, baseDirectory: File?): String {
        val portable = entry.replace('\\', '/')
        if (portable.startsWith("content://", ignoreCase = true)) return portable
        val file = File(portable)
        return if (file.isAbsolute || baseDirectory == null) portable else File(baseDirectory, portable).path
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')
        .replace(Regex("/+"), "/")
        .trimEnd('/')
        .lowercase()

    private fun decodeXml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
}
