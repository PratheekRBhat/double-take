package com.pratheekbhat.doubletake.domain.model

import com.pratheekbhat.doubletake.data.local.SyncedFileEntity

data class SyncActionItem(
    val relativePath: String,
    val action: SyncAction,
    val localFile: LocalFileMetadata?,
    val remoteFile: RemoteFileMetadata?,
    val dbRecord: SyncedFileEntity?
)
