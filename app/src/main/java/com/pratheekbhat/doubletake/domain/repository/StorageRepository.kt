package com.pratheekbhat.doubletake.domain.repository

import android.net.Uri
import com.pratheekbhat.doubletake.domain.model.LocalFileMetadata
import java.io.InputStream

interface StorageRepository {
    fun listLocalFiles(folderUri: Uri): Result<List<LocalFileMetadata>>
    fun readFile(fileUri: Uri): InputStream
    fun writeFile(folderUri: Uri, fileName: String, content: InputStream): Result<Uri>
    fun deleteFile(fileUri: Uri): Result<Unit>
    fun validateUriPermission(uri: Uri): Boolean
}