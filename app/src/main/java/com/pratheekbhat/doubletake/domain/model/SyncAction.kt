package com.pratheekbhat.doubletake.domain.model

enum class SyncAction {
    UPLOAD,
    DOWNLOAD,
    TRASH_LOCAL,
    TRASH_REMOTE,
    CONFLICT,
    NO_OP,
    CLEANUP_DB
}
