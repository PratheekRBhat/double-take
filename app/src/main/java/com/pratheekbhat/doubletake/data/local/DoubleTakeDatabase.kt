package com.pratheekbhat.doubletake.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SyncPairEntity::class, SyncedFileEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DoubleTakeDatabase : RoomDatabase() {
    abstract fun syncPairDao(): SyncPairDao
    abstract fun syncedFileDao(): SyncedFileDao
}