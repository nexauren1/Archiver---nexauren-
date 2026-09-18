package com.nexauren.file.core

import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ArchiveManager(private val storage: StorageRepository) {
    fun compressZip(sources: List<DocumentFile>, destination: DocumentFile, requestedName: String,
                    onProgress: (Int, Int, String) -> Unit): Result<DocumentFile> = runCatching {
        val requested = if (requestedName.endsWith(".zip", true)) requestedName else requestedName + ".zip"
        val target = storage.createFile(destination, "application/zip",
            storage.freeName(destination, requested)) ?: error("Não foi possível criar o ZIP.")

        val total = sources.sumOf { count(it) }
        var done = 0
        storage.openOutput(target)?.use { raw ->
            ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
                sources.forEach { source ->
                    add(source, source.name ?: "arquivo", zip) { label ->
                        done++
                        onProgress(done, total, label)
                    }
                }
            }
        }
        onProgress(total, total, "ZIP criado")
        target
    }

    fun extractZip(archive: DocumentFile, destination: DocumentFile,
                   onProgress: (Int, Int, String) -> Unit): Result<DocumentFile> = runCatching {
        require(archive.name?.endsWith(".zip", true) == true) {
            "Só é suportada a extração de ZIP nesta versão."
        }
        val base = archive.name!!.removeSuffix(".zip").ifBlank { "Extraído" }
        val target = storage.createDirectory(destination, storage.freeName(destination, base))
            ?: error("Não foi possível criar a pasta de destino.")

        var total = 0
        storage.openInput(archive)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    total++
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        var done = 0
        storage.openInput(archive)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val safe = safeEntry(entry.name)
                    val parts = safe.split('/').filter { it.isNotBlank() }
                    require(parts.isNotEmpty()) { "Entrada ZIP inválida." }
                    var parent = target
                    parts.dropLast(1).forEach { part ->
                        parent = storage.createDirectory(parent, part)
                            ?: error("Falha ao criar pasta.")
                    }
                    if (entry.isDirectory || safe.endsWith("/")) {
                        storage.createDirectory(parent, parts.last())
                    } else {
                        val file = storage.createFile(parent, storage.guessMime(parts.last()),
                            storage.freeName(parent, parts.last())) ?: error("Falha ao criar ficheiro.")
                        storage.openOutput(file)?.use { output -> zip.copyTo(output) }
                    }
                    done++
                    onProgress(done, total, safe)
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        target
    }

    private fun add(source: DocumentFile, path: String, zip: ZipOutputStream,
                    onFile: (String) -> Unit) {
        if (source.isDirectory) {
            val directory = if (path.endsWith("/")) path else path + "/"
            zip.putNextEntry(ZipEntry(directory))
            zip.closeEntry()
            source.listFiles().forEach { child ->
                add(child, path + "/" + (child.name ?: "arquivo"), zip, onFile)
            }
        } else {
            zip.putNextEntry(ZipEntry(path))
            storage.openInput(source)?.use { it.copyTo(zip) }
            zip.closeEntry()
            onFile(path)
        }
    }

    private fun count(file: DocumentFile): Int =
        if (file.isDirectory) file.listFiles().sumOf(::count).coerceAtLeast(1) else 1

    private fun safeEntry(name: String): String {
        val cleaned = name.replace('\\', '/')
        require(!cleaned.startsWith("/") && !cleaned.split('/').contains("..")) {
            "ZIP rejeitado por caminho inseguro."
        }
        return cleaned
    }
}
