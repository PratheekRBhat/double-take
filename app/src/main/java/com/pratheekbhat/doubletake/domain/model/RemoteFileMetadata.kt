package com.pratheekbhat.doubletake.domain.model

data class RemoteFileMetadata(
    val driveFileId: String,
    val fileName: String,
    val md5Checksum: String,
    val modifiedTime: Long
)

