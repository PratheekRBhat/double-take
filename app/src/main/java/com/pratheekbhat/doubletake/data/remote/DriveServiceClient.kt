package com.pratheekbhat.doubletake.data.remote

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveServiceClient @Inject constructor(
    private val context: Context,
    private val credential: GoogleAccountCredential
) {
    val driveService: Drive by lazy {
        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("DoubleTake")
            .build()
    }

    fun isPlayServicesAvailable(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}