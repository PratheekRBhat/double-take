package com.pratheekbhat.doubletake.domain.repository

import android.app.Activity
import android.app.PendingIntent
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isSignedIn: Flow<Boolean>
    suspend fun signIn(activityContext: Activity): Result<PendingIntent?>
    suspend fun signOut(): Result<Unit>
    fun getCredential(): GoogleAccountCredential?
}