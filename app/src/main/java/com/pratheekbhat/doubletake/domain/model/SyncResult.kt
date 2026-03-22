package com.pratheekbhat.doubletake.domain.model

data class SyncResult(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val trashedLocal: Int = 0,
    val trashedRemote: Int = 0,
    val conflicts: Int = 0,
    val failures: Int = 0
)
