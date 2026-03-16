package com.pratheekbhat.doubletake.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_pairs")
data class SyncPairEntity(
    @PrimaryKey(autoGenerate =  true) val id: Long = 0,
    val localFolderUri: String,
    val localFolderName: String,
    val driveFolderId: String,
    val driveFolderName: String,
    val createdAt: Long,
    val lastSyncedAt: Long? = null,
    val isEnabled: Boolean = true
)