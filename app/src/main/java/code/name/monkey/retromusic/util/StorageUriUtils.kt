package code.name.monkey.retromusic.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import java.io.File

/** Converts an ExternalStorageProvider tree URI into the path used by MediaStore.DATA. */
fun Uri.toLocalStorageFile(context: Context): File? {
    if (authority != EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY || !DocumentsContract.isTreeUri(this)) {
        return null
    }

    val documentId = runCatching { DocumentsContract.getTreeDocumentId(this) }.getOrNull()
        ?: return null
    return documentId.toLocalStorageFile(context)
}

/** Converts a single file URI returned by ExternalStorageProvider into a local path. */
fun Uri.toLocalStorageDocumentFile(context: Context): File? {
    if (authority != EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY ||
        !DocumentsContract.isDocumentUri(context, this)
    ) return null
    val documentId = runCatching { DocumentsContract.getDocumentId(this) }.getOrNull()
        ?: return null
    return documentId.toLocalStorageFile(context)
}

private fun String.toLocalStorageFile(context: Context): File? {
    val separator = indexOf(':')
    val volumeId = if (separator >= 0) substring(0, separator) else this
    val relativePath = if (separator >= 0) substring(separator + 1) else ""
    val volumeRoot = findVolumeRoot(context, volumeId) ?: return null
    val canonicalRoot = runCatching { volumeRoot.canonicalFile }.getOrNull() ?: return null
    val selectedFolder = runCatching {
        if (relativePath.isBlank()) canonicalRoot else File(canonicalRoot, relativePath).canonicalFile
    }.getOrNull() ?: return null

    val rootPath = canonicalRoot.path.trimEnd(File.separatorChar)
    val selectedPath = selectedFolder.path
    return selectedFolder.takeIf {
        selectedPath == rootPath || selectedPath.startsWith(rootPath + File.separator)
    }
}

@Suppress("DEPRECATION")
private fun findVolumeRoot(context: Context, volumeId: String): File? {
    if (volumeId.equals(PRIMARY_VOLUME_ID, ignoreCase = true)) {
        return Environment.getExternalStorageDirectory()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val storageManager = context.getSystemService(StorageManager::class.java)
        storageManager.storageVolumes.firstOrNull {
            it.uuid.equals(volumeId, ignoreCase = true)
        }?.directory?.let { return it }
    }

    context.getExternalFilesDirs(null).filterNotNull().forEach { appDirectory ->
        generateSequence(appDirectory) { it.parentFile }
            .firstOrNull { it.name.equals(volumeId, ignoreCase = true) }
            ?.let { return it }
    }

    return File("/storage", volumeId).takeIf { it.exists() && it.isDirectory }
}

private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY =
    "com.android.externalstorage.documents"
private const val PRIMARY_VOLUME_ID = "primary"
