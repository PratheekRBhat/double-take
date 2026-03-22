package com.pratheekbhat.doubletake.data.remote

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.pratheekbhat.doubletake.domain.model.RemoteFileMetadata
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveServiceClient @Inject constructor(
    private val context: Context,
    private val credential: GoogleAccountCredential
) {
    val driveService: Drive by lazy {
        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("DoubleTake")
            .build()
    }

    fun isPlayServicesAvailable(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }

    fun listRemoteFiles(folderId: String): Result<List<RemoteFileMetadata>> {
        return try {
            val files = mutableListOf<RemoteFileMetadata>()
            var pageToken: String? = null

            do {
                val result = driveService.files().list()
                    .setQ("'$folderId' in parents and trashed = false")
                    .setFields("nextPageToken, files(id, name, md5Checksum, modifiedTime)")
                    .setPageToken(pageToken)
                    .execute()

                result.files?.forEach { file ->
                    files.add(
                        RemoteFileMetadata(
                            driveFileId = file.id,
                            fileName = file.name,
                            md5Checksum = file.md5Checksum ?: "",
                            modifiedTime = file.modifiedTime?.value ?: 0L
                        )
                    )
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)

            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uploadFile(
        localUri: Uri,
        driveParentFolderId: String,
        existingDriveFileId: String?
    ): Result<RemoteFileMetadata> {
        return try {
            val inputStream = context.contentResolver.openInputStream(localUri)
                ?: return Result.failure(Exception("Cannot open local file: $localUri"))
            val mimeType = context.contentResolver.getType(localUri) ?: "application/octet-stream"
            val fileName = localUri.lastPathSegment ?: "unnamed"
            val content = InputStreamContent(mimeType, inputStream)

            val driveFile = if (existingDriveFileId != null) {
                driveService.files().update(existingDriveFileId, null, content)
                    .setFields("id, name, md5Checksum, modifiedTime")
                    .execute()
            } else {
                val metadata = File().apply {
                    name = fileName
                    parents = listOf(driveParentFolderId)
                }
                driveService.files().create(metadata, content)
                    .setFields("id, name, md5Checksum, modifiedTime")
                    .execute()
            }

            inputStream.close()

            Result.success(
                RemoteFileMetadata(
                    driveFileId = driveFile.id,
                    fileName = driveFile.name,
                    md5Checksum = driveFile.md5Checksum ?: "",
                    modifiedTime = driveFile.modifiedTime?.value ?: 0L
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun downloadFile(driveFileId: String): Result<InputStream> {
        return try {
            val inputStream = driveService.files().get(driveFileId)
                .executeMediaAsInputStream()
            Result.success(inputStream)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun trashFile(driveFileId: String): Result<Unit> {
        return try {
            val trashedFile = File().apply { trashed = true }
            driveService.files().update(driveFileId, trashedFile).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}