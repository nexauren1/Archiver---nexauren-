package com.nexauren.file.model

import androidx.documentfile.provider.DocumentFile

data class FileItem(
    val file: DocumentFile,
    val name: String,
    val uri: String,
    val isDirectory: Boolean,
    val size: Long,
    val modified: Long
)
