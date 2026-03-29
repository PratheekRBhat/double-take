package com.pratheekbhat.doubletake.di

import android.content.Context
import androidx.room.Room
import com.pratheekbhat.doubletake.data.local.DoubleTakeDatabase
import com.pratheekbhat.doubletake.data.local.SyncPairDao
import com.pratheekbhat.doubletake.data.local.SyncLogDao
import com.pratheekbhat.doubletake.data.local.SyncedFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DoubleTakeDatabase {
        return Room.databaseBuilder(
            context,
            DoubleTakeDatabase::class.java,
            "doubletake.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSyncPairDao(db: DoubleTakeDatabase): SyncPairDao = db.syncPairDao()

    @Provides
    fun provideSyncedFileDao(db: DoubleTakeDatabase): SyncedFileDao = db.syncedFileDao()

    @Provides
    fun provideSyncLogDao(db: DoubleTakeDatabase): SyncLogDao = db.syncLogDao()
}