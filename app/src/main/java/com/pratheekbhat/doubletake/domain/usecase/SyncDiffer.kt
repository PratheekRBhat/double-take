package com.pratheekbhat.doubletake.domain.usecase

import com.pratheekbhat.doubletake.data.local.SyncedFileEntity
import com.pratheekbhat.doubletake.domain.model.LocalFileMetadata
import com.pratheekbhat.doubletake.domain.model.RemoteFileMetadata
import com.pratheekbhat.doubletake.domain.model.SyncAction
import com.pratheekbhat.doubletake.domain.model.SyncActionItem
import javax.inject.Inject

class SyncDiffer @Inject constructor() {

    companion object {
        const val TIMESTAMP_TO_TOLERANCE_MS = 2000L
    }

    fun diff(
        localFiles: Map<String, LocalFileMetadata>,
        remoteFiles: Map<String, RemoteFileMetadata>,
        dbRecords: Map<String, SyncedFileEntity>
    ): List<SyncActionItem> {
        val allPaths = localFiles.keys + remoteFiles.keys + dbRecords.keys

        return allPaths.map { path ->
            val local = localFiles[path]
            val remote = remoteFiles[path]
            val db = dbRecords[path]

            val action = when {
                // All three present. Check what changed
                local != null && remote != null && db!= null -> {
                    val localChanged = local.lastModified > db.localModifiedAt
                    val remoteChanged = remote.modifiedTime > db.remoteModifiedAt
                    when {
                        localChanged && remoteChanged -> resolveConflict(local, remote)
                        localChanged -> SyncAction.UPLOAD
                        remoteChanged -> SyncAction.DOWNLOAD
                        else -> SyncAction.NO_OP
                    }
                }

                // New on both sides, not in DB
                local != null && remote != null && db == null -> {
                    resolveConflict(local, remote)
                }

                // In local and DB, gone from remote (remote deleted it)
                local != null && remote == null && db != null -> SyncAction.TRASH_LOCAL

                // In remote + DB, gone from local (local deleted it)
                local == null && remote != null && db != null -> SyncAction.TRASH_REMOTE

                // Local only (new file)
                local != null && remote == null && db == null -> SyncAction.UPLOAD

                // Remote only (new file)
                local == null && remote != null && db == null -> SyncAction.DOWNLOAD

                // DB only (deleted from both sides)
                local == null && remote == null && db != null -> SyncAction.CLEANUP_DB

                else -> SyncAction.NO_OP
            }

            SyncActionItem(
                relativePath = path,
                action = action,
                localFile = local,
                remoteFile = remote,
                dbRecord = db
            )
        }
    }

    private fun resolveConflict(
        local: LocalFileMetadata,
        remote: RemoteFileMetadata
    ): SyncAction {
        val timeDifference = local.lastModified - remote.modifiedTime
        return when {
            timeDifference > TIMESTAMP_TO_TOLERANCE_MS -> SyncAction.UPLOAD
            timeDifference < -TIMESTAMP_TO_TOLERANCE_MS -> SyncAction.DOWNLOAD
            else -> SyncAction.CONFLICT
        }
    }
}
