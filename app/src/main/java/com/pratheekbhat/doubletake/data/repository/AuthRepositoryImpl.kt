package com.pratheekbhat.doubletake.data.repository

import android.accounts.AccountManager
import android.app.Activity
import android.app.PendingIntent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.pratheekbhat.doubletake.data.local.AuthDataStore
import com.pratheekbhat.doubletake.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val credential: GoogleAccountCredential
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> = authDataStore.isSignedIn

    override suspend fun signIn(activityContext: Activity): Result<PendingIntent?> {
        return try {
            val authRequest = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(DriveScopes.DRIVE))).build()
            val authClient = Identity.getAuthorizationClient(activityContext)
            val result = authClient.authorize(authRequest).await()

            Log.d("[AuthRepository]", "hasResolution=${result.hasResolution()}")
            Log.d("[AuthRepository]", "serverAuthCode=${result.serverAuthCode}")
            Log.d("[AuthRepository]", "grantedScopes=${result.grantedScopes}")
            Log.d("[AuthRepository]", "accessToken=${result.accessToken}")

            if (result.hasResolution()) {
                Result.success(result.pendingIntent)
            } else {
                if (result.grantedScopes.isNotEmpty()) {
                    val accountManager = AccountManager.get(activityContext)
                    val accounts = accountManager.getAccountsByType("com.google")
                    val accountName = accounts.firstOrNull()?.name

                    if (accountName != null) {
                        credential.selectedAccountName = accountName
                        authDataStore.saveAuthState(accountName)
                    } else {
                        authDataStore.saveAuthState("authorized")
                    }
                    Result.success(null)
                } else {
                    Result.failure(Exception("Authorization not granted"))
                }
            }
        } catch (e: Exception) {
            Log.e("[AuthRepository]", "signIn exception", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            credential.selectedAccountName = null
            authDataStore.clearAuthState()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCredential(): GoogleAccountCredential = credential
}