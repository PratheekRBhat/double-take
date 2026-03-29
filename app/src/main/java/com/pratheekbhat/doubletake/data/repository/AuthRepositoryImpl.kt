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
            val authRequest = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(DriveScopes.DRIVE), Scope("email"))).build()
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
                    val accessToken = result.accessToken
                    if (accessToken != null) {
                        val email = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { fetchEmailFromToken(accessToken) }
                        if (email != null) {
                            credential.selectedAccountName = email
                            authDataStore.saveAuthState(email)
                            Log.d("[AuthRepository]", "Credential set to: $email")
                        } else {
                            Log.w("[AuthRepository]", "Could not resolve email from token")
                        }
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

    private fun fetchEmailFromToken(accessToken: String): String? {
        return try {
            val url = java.net.URL("https://www.googleapis.com/oauth2/v3/userinfo")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val emailRegex = """"email"\s*:\s*"([^"]+)"""".toRegex()
                emailRegex.find(response)?.groupValues?.get(1)
            } else {
                Log.e("[AuthRepository]", "Tokeninfo call failed: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("[AuthRepository]", "Failed to fetch email from token", e)
            null
        }
    }
}