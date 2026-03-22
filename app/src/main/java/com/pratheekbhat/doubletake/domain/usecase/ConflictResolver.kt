package com.pratheekbhat.doubletake.domain.usecase

import com.pratheekbhat.doubletake.domain.model.LocalFileMetadata
import com.pratheekbhat.doubletake.domain.model.RemoteFileMetadata
import com.pratheekbhat.doubletake.domain.model.SyncAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ConflictResult(
    val action: SyncAction,
    val conflictCopyName: String? = null
)

data class ConflictEvent(
    val relativePath: String,
    val localModifiedAt: Long,
    val remoteModifiedAt: Long,
    val resolution: String
)

class ConflictResolver @Inject constructor() {

    companion object {
        const val TIMESTAMP_TOLERANCE_MS = 2000L
    }

    private val _conflictLog = mutableListOf<ConflictEvent>()
    val conflictLog: List<ConflictEvent> get() = _conflictLog

    fun clearLog() {
        _conflictLog.clear()
    }

    fun resolve(
        relativePath: String,
        local: LocalFileMetadata,
        remote: RemoteFileMetadata
    ): ConflictResult {
        val timeDifference = local.lastModified - remote.modifiedTime

        return when {
            timeDifference > TIMESTAMP_TOLERANCE_MS -> {
                logEvent(relativePath, local.lastModified, remote.modifiedTime, "LOCAL_WINS")
                ConflictResult(SyncAction.UPLOAD)
            }
            timeDifference < -TIMESTAMP_TOLERANCE_MS -> {
                logEvent(relativePath, local.lastModified, remote.modifiedTime, "REMOTE_WINS")
                ConflictResult(SyncAction.DOWNLOAD)
            }
            // TODO: Compare MD5 hashes directly once we compute local file hashes.
            //  Currently assumes content differs when timestamps are within tolerance.
            local.mimeType != null && remote.md5Checksum.isNotEmpty() -> {
                val copyName = buildConflictCopyName(relativePath)
                logEvent(relativePath, local.lastModified, remote.modifiedTime, "CONFLICT_COPY: $copyName")
                ConflictResult(action = SyncAction.CONFLICT, conflictCopyName = copyName)
            }
            else -> {
                logEvent(relativePath, local.lastModified, remote.modifiedTime, "SAME_CONTENT")
                ConflictResult(SyncAction.NO_OP)
            }
        }
    }

    fun buildConflictCopyName(relativePath: String): String {
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lastDot = relativePath.lastIndexOf('.')
        return if (lastDot > 0) {
            val name = relativePath.take(lastDot)
            val extension = relativePath.substring(lastDot)
            "$name (conflict $date)$extension"
        } else {
            "$relativePath (conflict $date)"
        }
    }

    private fun logEvent(
        relativePath: String,
        localModifiedAt: Long,
        remoteModifiedAt: Long,
        resolution: String
    ) {
        _conflictLog.add(
            ConflictEvent(
                relativePath = relativePath,
                localModifiedAt = localModifiedAt,
                remoteModifiedAt = remoteModifiedAt,
                resolution = resolution
            )
        )
    }
}