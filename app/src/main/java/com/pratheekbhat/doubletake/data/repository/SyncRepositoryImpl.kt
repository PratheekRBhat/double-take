package com.pratheekbhat.doubletake.data.repository

import android.util.Log
import com.pratheekbhat.doubletake.data.local.SyncLogDao
import com.pratheekbhat.doubletake.data.local.SyncLogEntity
import com.pratheekbhat.doubletake.data.local.SyncPairDao
import com.pratheekbhat.doubletake.data.local.SyncedFileDao
import com.pratheekbhat.doubletake.data.local.SyncedFileEntity
import com.pratheekbhat.doubletake.data.remote.DriveServiceClient
import com.pratheekbhat.doubletake.domain.model.LocalFileMetadata
import com.pratheekbhat.doubletake.domain.model.RemoteFileMetadata
import com.pratheekbhat.doubletake.domain.model.SyncAction
import com.pratheekbhat.doubletake.domain.model.SyncResult
import com.pratheekbhat.doubletake.domain.model.SyncStatus
import com.pratheekbhat.doubletake.domain.repository.StorageRepository
import com.pratheekbhat.doubletake.domain.repository.SyncRepository
import com.pratheekbhat.doubletake.domain.usecase.ConflictResolver
import com.pratheekbhat.doubletake.domain.usecase.SyncDiffer
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncPairDao: SyncPairDao,
    private val syncedFileDao: SyncedFileDao,
    private val syncLogDao: SyncLogDao,
    private val driveServiceClient: DriveServiceClient,
    private val storageRepository: StorageRepository,
    private val syncDiffer: SyncDiffer,
    private val conflictResolver: ConflictResolver
) : SyncRepository {
    override suspend fun performSync(syncPairId: Long): Result<SyncResult> {
        return try {
            val pair = syncPairDao.getById(syncPairId)
                ?: return Result.failure(Exception("SyncPair not found: $syncPairId"))

            val localFiles = storageRepository.listLocalFiles(pair.localFolderUri.toUri())
                .getOrElse { return Result.failure(it) }
                .associateBy { it.name }

            val remoteFiles = driveServiceClient.listRemoteFiles(pair.driveFolderId)
                .getOrElse { return Result.failure(it) }
                .associateBy { it.fileName }

            val dbRecords = syncedFileDao.getAllBySyncPair(syncPairId).first()
                .associateBy { it.relativePath }

            val actions = syncDiffer.diff(localFiles, remoteFiles, dbRecords)

            var uploaded = 0
            var downloaded = 0
            var trashedLocal = 0
            var trashedRemote = 0
            var conflicts = 0
            var failures = 0

            for (item in actions) {
                try {
                    when (item.action) {
                        SyncAction.UPLOAD -> {
                            val local = item.localFile!!
                            val result = driveServiceClient.uploadFile(
                                localUri = local.uri,
                                driveParentFolderId = pair.driveFolderId,
                                existingDriveFileId = item.remoteFile?.driveFileId
                            ).getOrThrow()
                            upsertDbRecord(syncPairId, item.relativePath, local, result, SyncStatus.SYNCED)
                            writeLog(syncPairId, "UPLOAD", item.relativePath, "SUCCESS", "Uploaded to Drive", pair.localFolderName, pair.driveFolderName)
                            uploaded++
                        }

                        SyncAction.DOWNLOAD -> {
                            val remote = item.remoteFile!!
                            val inputStream = driveServiceClient.downloadFile(remote.driveFileId).getOrThrow()
                            val folderUri = pair.localFolderUri.toUri()
                            storageRepository.writeFile(folderUri, remote.fileName, inputStream).getOrThrow()
                            inputStream.close()

                            val localFiles = storageRepository.listLocalFiles(folderUri).getOrThrow()
                            val downloadedLocal = localFiles.find { it.name == remote.fileName }
                            upsertDbRecord(syncPairId, item.relativePath, downloadedLocal, remote, SyncStatus.SYNCED)
                            writeLog(syncPairId, "DOWNLOAD", item.relativePath, "SUCCESS", "Downloaded from Drive", pair.driveFolderName, pair.localFolderName)
                            downloaded++
                        }

                        SyncAction.TRASH_LOCAL -> {
                            val db = item.dbRecord!!
                            if (db.localUri != null) {
                                storageRepository.deleteFile(db.localUri.toUri())
                            }
                            syncedFileDao.deleteByRelativePath(syncPairId, item.relativePath)
                            writeLog(syncPairId, "TRASH_LOCAL", item.relativePath, "SUCCESS", "Deleted locally (remote was removed)", pair.driveFolderName, pair.localFolderName)
                            trashedLocal++
                        }

                        SyncAction.TRASH_REMOTE -> {
                            val db = item.dbRecord!!
                            if (db.driveFileId != null) {
                                driveServiceClient.trashFile(db.driveFileId).getOrThrow()
                            }
                            syncedFileDao.deleteByRelativePath(syncPairId, item.relativePath)
                            writeLog(syncPairId, "TRASH_REMOTE", item.relativePath, "SUCCESS", "Trashed on Drive (local was removed)", pair.localFolderName, pair.driveFolderName)
                            trashedRemote++
                        }

                        SyncAction.CONFLICT -> {
                            val local = item.localFile!!
                            val remote = item.remoteFile!!
                            val resolution = conflictResolver.resolve(item.relativePath, local, remote)
                            when (resolution.action) {
                                SyncAction.UPLOAD -> {
                                    val result = driveServiceClient.uploadFile(
                                        local.uri,
                                        pair.driveFolderId,
                                        remote.driveFileId
                                    ).getOrThrow()
                                    upsertDbRecord(syncPairId, item.relativePath, local, result, SyncStatus.SYNCED)
                                    writeLog(syncPairId, "CONFLICT", item.relativePath, "SUCCESS", "Conflict resolved: local wins", pair.localFolderName, pair.driveFolderName)
                                    uploaded++
                                }
                                SyncAction.DOWNLOAD -> {
                                    val inputStream = driveServiceClient.downloadFile(remote.driveFileId).getOrThrow()
                                    storageRepository.writeFile(pair.localFolderUri.toUri(), remote.fileName, inputStream).getOrThrow()
                                    inputStream.close()
                                    upsertDbRecord(syncPairId, item.relativePath, null, remote, SyncStatus.SYNCED)
                                    writeLog(syncPairId, "CONFLICT", item.relativePath, "SUCCESS", "Conflict resolved: remote wins", pair.driveFolderName, pair.localFolderName)
                                    downloaded++
                                }
                                SyncAction.CONFLICT -> {
                                    val copyName = resolution.conflictCopyName!!
                                    val inputStream = driveServiceClient.downloadFile(remote.driveFileId).getOrThrow()
                                    storageRepository.writeFile(
                                        pair.localFolderUri.toUri(), copyName, inputStream
                                    ).getOrThrow()
                                    inputStream.close()
                                    driveServiceClient.uploadFile(
                                        localUri = local.uri,
                                        driveParentFolderId = pair.driveFolderId,
                                        existingDriveFileId = remote.driveFileId
                                    ).getOrThrow()
                                    upsertDbRecord(syncPairId, item.relativePath, local, remote, SyncStatus.CONFLICT)
                                    writeLog(syncPairId, "CONFLICT", item.relativePath, "SUCCESS", "Conflict: both versions kept", pair.localFolderName, pair.driveFolderName)
                                    conflicts++
                                }
                                else -> {}
                            }
                        }

                        SyncAction.LINK_EXISTING -> {
                            val local = item.localFile
                            val remote = item.remoteFile
                            upsertDbRecord(syncPairId, item.relativePath, local, remote, SyncStatus.SYNCED)
                        }

                        SyncAction.CLEANUP_DB -> {
                            syncedFileDao.deleteByRelativePath(syncPairId, item.relativePath)
                        }

                        SyncAction.NO_OP -> {}
                    }
                } catch (e: Exception) {
                    Log.e("[SyncRepository]", "Failed to sync ${item.relativePath}", e)
                    writeLog(syncPairId, item.action.name, item.relativePath, "FAILURE", e.message ?: "Unknown error", pair.localFolderName, pair.driveFolderName)
                    failures++
                }
            }
            syncPairDao.updateLastSyncedAt(syncPairId, System.currentTimeMillis())
            syncLogDao.deleteOldestBeyond()

            Result.success(
                SyncResult(
                    uploaded = uploaded,
                    downloaded = downloaded,
                    trashedLocal = trashedLocal,
                    trashedRemote = trashedRemote,
                    conflicts = conflicts,
                    failures = failures
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun performSyncAll(): Result<Map<Long, SyncResult>> {
        return try {
            val pairs = syncPairDao.getAllEnabled().first()
            val results = mutableMapOf<Long, SyncResult>()
            for (pair in pairs) {
                val result = performSync(pair.id)
                results[pair.id] = result.getOrElse {
                    SyncResult(failures = 1)
                }
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun writeLog(
        syncPairId: Long,
        action: String,
        filePath: String,
        result: String,
        details: String?,
        sourceName: String?,
        destinationName: String?
    ) {
        try {
            syncLogDao.insertLog(
                SyncLogEntity(
                    syncPairId = syncPairId,
                    timestamp = System.currentTimeMillis(),
                    action = action,
                    filePath = filePath,
                    result = result,
                    details = details,
                    sourceName = sourceName,
                    destinationName = destinationName
                )
            )
        } catch (e: Exception) {
            Log.e("[SyncRepository]", "Failed to write sync log", e)
        }
    }

    private suspend fun upsertDbRecord(
        syncPairId: Long,
        relativePath: String,
        local: LocalFileMetadata?,
        remote: RemoteFileMetadata?,
        status: SyncStatus
    ) {
        syncedFileDao.insertOrReplace(
            SyncedFileEntity(
                syncPairId = syncPairId,
                fileName = local?.name ?: remote?.fileName ?: relativePath,
                relativePath = relativePath,
                localUri = local?.uri?.toString(),
                driveFileId = remote?.driveFileId,
                md5Hash = remote?.md5Checksum,
                localModifiedAt = local?.lastModified ?: 0L,
                remoteModifiedAt = remote?.modifiedTime ?: 0L,
                lastSyncedAt = System.currentTimeMillis(),
                syncStatus = status
            )
        )
    }
}