/*
 * Copyright (c) 2026 Metro contributors.
 * Licensed under the GNU General Public License v3.
 */
package code.name.monkey.retromusic.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import code.name.monkey.retromusic.CUSTOM_FONT
import code.name.monkey.retromusic.CUSTOM_FONT_NAME
import java.io.File
import java.util.WeakHashMap

object CustomFontManager {
    private const val FILE_NAME = "custom-font"
    private const val MAX_FONT_SIZE = 20L * 1024L * 1024L

    fun hasCustomFont(context: Context): Boolean = fontFile(context).isFile

    fun load(context: Context): Typeface? = try {
        fontFile(context).takeIf(File::isFile)?.let(Typeface::createFromFile)
    } catch (_: RuntimeException) {
        null
    }

    fun install(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex) && cursor.getLong(sizeIndex) > MAX_FONT_SIZE) {
                    throw IllegalArgumentException("Font file is too large")
                }
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
            ?.takeIf(String::isNotBlank)
            ?: "custom-font"

        val temporary = File(context.cacheDir, "$FILE_NAME.tmp")
        try {
            resolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalArgumentException("Unable to open font")
            if (temporary.length() == 0L || temporary.length() > MAX_FONT_SIZE) {
                throw IllegalArgumentException("Invalid font size")
            }
            Typeface.createFromFile(temporary)
            temporary.inputStream().use { input ->
                fontFile(context).outputStream().use { output -> input.copyTo(output) }
            }
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putBoolean(CUSTOM_FONT, true)
                putString(CUSTOM_FONT_NAME, displayName)
            }
            return displayName
        } finally {
            temporary.delete()
        }
    }

    fun reset(context: Context) {
        fontFile(context).delete()
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(CUSTOM_FONT, false)
            remove(CUSTOM_FONT_NAME)
        }
    }

    fun attach(root: View): FontApplier? = load(root.context)?.let { FontApplier(root, it).also(FontApplier::start) }

    private fun fontFile(context: Context) = File(context.filesDir, FILE_NAME)
}

class FontApplier(
    private val root: View,
    private val typeface: Typeface,
) : ViewTreeObserver.OnGlobalLayoutListener {
    private val appliedViews = WeakHashMap<TextView, Boolean>()
    private var scanPosted = false

    fun start() {
        root.viewTreeObserver.addOnGlobalLayoutListener(this)
        scheduleScan()
    }

    fun stop() {
        if (root.viewTreeObserver.isAlive) {
            root.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
        appliedViews.clear()
    }

    override fun onGlobalLayout() = scheduleScan()

    private fun scheduleScan() {
        if (scanPosted) return
        scanPosted = true
        root.post {
            scanPosted = false
            applyTo(root)
        }
    }

    private fun applyTo(view: View) {
        if (view is TextView && appliedViews.put(view, true) == null) {
            val style = view.typeface?.style ?: Typeface.NORMAL
            view.typeface = Typeface.create(typeface, style)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyTo(view.getChildAt(index))
        }
    }
}
