package com.pratheekbhat.doubletake.domain.model

import android.net.Uri

data class LocalFileMetadata(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?
)