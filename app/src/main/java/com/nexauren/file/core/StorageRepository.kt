package com.nexauren.file.core

import android.content.ContentResolver
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.nexauren.file.model.FileItem
import java.io.IOException

class StorageRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun fromUri(uri: String): DocumentFile? =
        DocumentFile.fromTreeUri(context, android.net.Uri.parse(uri))
            ?: DocumentFile.fromSingleUri(context, android.net.Uri.parse(uri))

    fun list(directory: DocumentFile): List<FileItem> =
        directory.listFiles().map {
            FileItem(it, it.name ?: "(sem nome)", it.uri.toString(), it.isDirectory,
                if (it.isFile) it.length() else 0L, it.lastModified())
        }.sortedWith(compareBy<FileItem> { !it.isDirectory }
            .thenBy { it.name.lowercase() })

    fun createDirectory(parent: DocumentFile, name: String): DocumentFile? =
        parent.findFile(name)?.takeIf { it.isDirectory } ?: parent.createDirectory(name)

    fun createFile(parent: DocumentFile, mime: String, name: String): DocumentFile? =
        parent.findFile(name) ?: parent.createFile(mime, name)

    fun delete(file: DocumentFile): Boolean = runCatching { file.delete() }.getOrDefault(false)

    fun rename(file: DocumentFile, newName: String): Boolean =
        runCatching { file.renameTo(newName) }.getOrDefault(false)

    fun copyRecursive(source: DocumentFile, destination: DocumentFile): DocumentFile? {
        if (source.isDirectory) {
            val dir = createDirectory(destination, source.name ?: "Pasta") ?: return null
            source.listFiles().forEach { child -> copyRecursive(child, dir) }
            return dir
        }
        val target = createFile(destination, source.type ?: "application/octet-stream",
            source.name ?: "ficheiro") ?: return null
        resolver.openInputStream(source.uri)?.use { input ->
            resolver.openOutputStream(target.uri)?.use { output -> input.copyTo(output) }
        }
        return target
    }

    fun openInput(file: DocumentFile) = resolver.openInputStream(file.uri)
    fun openOutput(file: DocumentFile) = resolver.openOutputStream(file.uri)

    fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }

    fun freeName(parent: DocumentFile, requested: String): String {
        if (parent.findFile(requested) == null) return requested
        val baseName = requested.substringBeforeLast('.', requested)
        val ext = requested.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }
        for (i in 1..999) {
            val candidate = baseName + " (" + i + ")" + ext
            if (parent.findFile(candidate) == null) return candidate
        }
        throw IOException("Não foi possível criar um nome livre.")
    }
}
