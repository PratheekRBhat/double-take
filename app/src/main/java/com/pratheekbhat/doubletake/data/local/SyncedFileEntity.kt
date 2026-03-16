package com.pratheekbhat.doubletake.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pratheekbhat.doubletake.domain.model.SyncStatus

@Entity(
    tableName = "synced_files",
    indices = [Index(value = ["syncPairId", "relativePath"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = SyncPairEntity::class,
            parentColumns = ["id"],
            childColumns = ["syncPairId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SyncedFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncPairId: Long,
    val fileName: String,
    val relativePath: String,
    val localUri: String?,
    val driveFileId: String?,
    val md5Hash: String?,
    val localModifiedAt: Long,
    val remoteModifiedAt: Long,
    val lastSyncedAt: Long?,
    val syncStatus: SyncStatus
)