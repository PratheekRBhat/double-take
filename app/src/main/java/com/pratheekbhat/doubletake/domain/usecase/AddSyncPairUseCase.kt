package com.pratheekbhat.doubletake.domain.usecase

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import com.pratheekbhat.doubletake.domain.repository.SyncPairRepository
import javax.inject.Inject

class AddSyncPairUseCase @Inject constructor(
    private val contentResolver: ContentResolver,
    private val syncPairRepository: SyncPairRepository
) {

    suspend operator fun invoke(
        localFolderUri: Uri,
        localFolderName: String,
        driveFolderId: String,
        driveFolderName: String
    ): Result<SyncPairEntity> {
        return try {
            contentResolver.takePersistableUriPermission(
                localFolderUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            syncPairRepository.createPair(
                localUri = localFolderUri.toString(),
                localName = localFolderName,
                driveId = driveFolderId,
                driveName = driveFolderName
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}