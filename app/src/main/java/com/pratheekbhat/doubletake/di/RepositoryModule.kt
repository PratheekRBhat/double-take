package com.pratheekbhat.doubletake.di

import android.content.ContentResolver
import android.content.Context
import com.pratheekbhat.doubletake.data.repository.AuthRepositoryImpl
import com.pratheekbhat.doubletake.data.repository.StorageRepositoryImpl
import com.pratheekbhat.doubletake.data.repository.SyncPairRepositoryImpl
import com.pratheekbhat.doubletake.data.repository.SyncRepositoryImpl
import com.pratheekbhat.doubletake.domain.repository.AuthRepository
import com.pratheekbhat.doubletake.domain.repository.StorageRepository
import com.pratheekbhat.doubletake.domain.repository.SyncPairRepository
import com.pratheekbhat.doubletake.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    abstract fun bindSyncPairRepository(impl: SyncPairRepositoryImpl): SyncPairRepository

    @Binds
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    companion object {
        @Provides
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
            return context.contentResolver
        }
    }
}