package com.pratheekbhat.doubletake.presentation.setup

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratheekbhat.doubletake.data.local.AuthDataStore
import com.pratheekbhat.doubletake.domain.usecase.AddSyncPairUseCase
import com.pratheekbhat.doubletake.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val isDriveConnected: Boolean = false,
    val isLocalFolderChosen: Boolean = false,
    val localFolderUri: String? = null,
    val localFolderName: String? = null,
    val isDriveFolderSelected: Boolean = false,
    val driveFolderId: String? = null,
    val driveFolderName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val addSyncPairUseCase: AddSyncPairUseCase,
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _authIntentEvent = MutableSharedFlow<IntentSenderRequest>(extraBufferCapacity = 1)
    val authIntentEvent: SharedFlow<IntentSenderRequest> = _authIntentEvent.asSharedFlow()

    fun onConnectDrive(activity: Activity) {
        Log.d("[SetupViewModel]", "onConnectDrive called")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            signInUseCase(activity)
                .onSuccess { pendingIntent ->
                    Log.d("[SetupViewModel]", "signIn success, pendingIntent=$pendingIntent")
                    if (pendingIntent != null) {
                        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        _authIntentEvent.emit(request)
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        _uiState.update { it.copy(isDriveConnected = true, isLoading = false) }
                    }
                }
                .onFailure { exception ->
                    Log.e("[SetupViewModel]", "signIn failed", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    fun onLocalFolderChosen(uri: Uri, folderName: String) {
        _uiState.update {
            it.copy(
                isLocalFolderChosen = true,
                localFolderUri = uri.toString(),
                localFolderName = folderName
            )
        }
    }

    fun onDriveFolderSelected(folderId: String, folderName: String) {
        _uiState.update {
            it.copy(
                isDriveFolderSelected = true,
                driveFolderId = folderId,
                driveFolderName = folderName
            )
        }
    }

    fun onStartSync() {
        val state = _uiState.value
        val localUri = state.localFolderUri ?: return
        val driveFolderId = state.driveFolderId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            addSyncPairUseCase(
                localFolderUri = localUri.toUri(),
                driveFolderId = driveFolderId,
                localFolderName = state.localFolderName ?: "Local Folder",
                driveFolderName = state.driveFolderName ?: "Drive Folder"
            )
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    fun onAuthResult(intent: android.content.Intent?) {
        viewModelScope.launch {
            if (intent == null) {
                _uiState.update { it.copy(error = "Sign in cancelled") }
                return@launch
            }
            authDataStore.saveAuthState("signed_in")
            _uiState.update { it.copy(isDriveConnected = true) }
        }
    }
}