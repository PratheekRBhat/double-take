package com.pratheekbhat.doubletake.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_logs",
    foreignKeys = [
        ForeignKey(
            entity = SyncPairEntity::class,
            parentColumns = ["id"],
            childColumns = ["syncPairId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("syncPairId")]
)
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncPairId: Long,
    val timestamp: Long,
    val action: String, // UPLOAD, DOWNLOAD, TRASH_LOCAL, TRASH_REMOTE, CONFLICT
    val filePath: String,
    val result: String, // SUCCESS, FAILURE
    val details: String? = null,
    val sourceName: String? = null,
    val destinationName: String? = null
)
