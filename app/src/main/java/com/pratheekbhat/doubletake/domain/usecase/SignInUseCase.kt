package com.pratheekbhat.doubletake.domain.usecase

import android.app.Activity
import com.pratheekbhat.doubletake.data.remote.DriveServiceClient
import com.pratheekbhat.doubletake.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val driveServiceClient: DriveServiceClient
) {
    suspend operator fun invoke(activityContext: Activity): Result<Unit> {
        if (!driveServiceClient.isPlayServicesAvailable()) {
            return Result.failure(Exception("Google Play Services is not available"))
        }
        return authRepository.signIn(activityContext)
    }
}