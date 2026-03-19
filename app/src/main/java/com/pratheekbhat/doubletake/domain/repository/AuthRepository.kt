package com.pratheekbhat.doubletake.domain.repository

import android.app.Activity
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isSignedIn: Flow<Boolean>
    suspend fun signIn(activityContext: Activity): Result<Unit>
    suspend fun signOut(): Result<Unit>
    fun getCredential(): GoogleAccountCredential?
}