package com.pratheekbhat.doubletake.domain.model

enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DOWNLOAD,
    CONFLICT,
    PENDING_DELETE
}