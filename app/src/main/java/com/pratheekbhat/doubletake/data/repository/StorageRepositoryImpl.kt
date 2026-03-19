package com.pratheekbhat.doubletake.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pratheekbhat.doubletake.domain.model.LocalFileMetadata
import com.pratheekbhat.doubletake.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageRepository {

    override fun listLocalFiles(folderUri: Uri): Result<List<LocalFileMetadata>> {
        return try {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
                ?: return Result.failure(Exception("Invalid folder URI"))

            val files = folder.listFiles()
                .filter { it.isFile }
                .map { file ->
                    LocalFileMetadata(
                        uri = file.uri,
                        name = file.name ?: "unknown",
                        size = file.length(),
                        lastModified = file.lastModified(),
                        mimeType = file.type
                    )
                }
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun readFile(fileUri: Uri): InputStream {
        return context.contentResolver.openInputStream(fileUri)
            ?: throw IllegalArgumentException("Cannot open input stream for URI: $fileUri")
    }

    override fun writeFile(folderUri: Uri, fileName: String, content: InputStream): Result<Uri> {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: return Result.failure(Exception("Invalid folder URI"))

        val existingFile = folder.findFile(fileName)
        val targetFile = existingFile ?: folder.createFile("application/octet-stream", fileName)
            ?: return Result.failure(Exception("Failed to create file: $fileName"))

        context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
            content.copyTo(output)
        } ?: return Result.failure(Exception("Cannot open output stream for: ${targetFile.uri}"))

        return Result.success(targetFile.uri)
    }

    override fun deleteFile(fileUri: Uri): Result<Unit> {
        return try {
            val file = DocumentFile.fromSingleUri(context, fileUri) ?: return Result.failure(Exception("Invalid file URI"))

            if (file.delete()) Result.success(Unit)
            else Result.failure(Exception("Failed to delete file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun validateUriPermission(uri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }
}