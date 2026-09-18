package com.nexauren.file.core

import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class OperationType { COPY, MOVE, DELETE, COMPRESS, EXTRACT }

data class OperationProgress(
    val type: OperationType,
    val current: Int,
    val total: Int,
    val message: String
)

class OperationManager(private val storage: StorageRepository) {
    suspend fun copy(sources: List<DocumentFile>, destination: DocumentFile,
                     onProgress: (OperationProgress) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                sources.forEachIndexed { i, source ->
                    onProgress(OperationProgress(OperationType.COPY, i, sources.size, source.name ?: ""))
                    storage.copyRecursive(source, destination)
                }
                onProgress(OperationProgress(OperationType.COPY, sources.size, sources.size, "Concluído"))
            }
        }

    suspend fun move(sources: List<DocumentFile>, destination: DocumentFile,
                     onProgress: (OperationProgress) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                sources.forEachIndexed { i, source ->
                    onProgress(OperationProgress(OperationType.MOVE, i, sources.size, source.name ?: ""))
                    storage.copyRecursive(source, destination)
                    storage.delete(source)
                }
                onProgress(OperationProgress(OperationType.MOVE, sources.size, sources.size, "Concluído"))
            }
        }

    suspend fun delete(sources: List<DocumentFile>,
                       onProgress: (OperationProgress) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                sources.forEachIndexed { i, source ->
                    onProgress(OperationProgress(OperationType.DELETE, i, sources.size, source.name ?: ""))
                    storage.delete(source)
                }
                onProgress(OperationProgress(OperationType.DELETE, sources.size, sources.size, "Concluído"))
            }
        }
}
