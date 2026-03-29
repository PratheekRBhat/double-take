package com.pratheekbhat.doubletake.presentation.setup

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.pratheekbhat.doubletake.data.local.AuthDataStore
import com.pratheekbhat.doubletake.data.remote.DriveServiceClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriveFolderPickerUiState(
    val folders: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val consentIntent: Intent? = null
)

@HiltViewModel
class DriveFolderPickerViewModel @Inject constructor(
    private val driveServiceClient: DriveServiceClient,
    private val authDataStore: AuthDataStore,
    private val credential: GoogleAccountCredential
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveFolderPickerUiState())
    val uiState: StateFlow<DriveFolderPickerUiState> = _uiState.asStateFlow()

    init {
        restoreCredentialAndLoadFolders()
    }

    fun restoreCredentialAndLoadFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            authDataStore.accountName.first().let { name ->
                if (name != null) {
                    credential.selectedAccountName = name
                }
            }
            loadFolders()
        }
    }

    fun loadFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null, consentIntent = null) }
            driveServiceClient.listFolders()
                .onSuccess { folders ->
                    _uiState.update { it.copy(folders = folders, isLoading = false) }
                }
                .onFailure { exception ->
                    Log.e("[DriveFolderPicker]", "Failed to load folders", exception)
                    val cause = if (exception is UserRecoverableAuthIOException) exception
                        else (exception.cause as? UserRecoverableAuthIOException)
                    if (cause != null) {
                        Log.d("[DriveFolderPicker]", "Consent needed, launching recovery intent")
                        _uiState.update { it.copy(isLoading = false, consentIntent = cause.intent) }
                    } else {
                        _uiState.update { it.copy(error = exception.message, isLoading = false) }
                    }
                }
        }
    }

    fun onConsentResult(success: Boolean) {
        _uiState.update { it.copy(consentIntent = null) }
        if (success) {
            loadFolders()
        } else {
            _uiState.update { it.copy(error = "Drive access denied") }
        }
    }

    fun clearConsentIntent() {
        _uiState.update { it.copy(consentIntent = null) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            driveServiceClient.createFolder(name)
                .onSuccess { loadFolders() }
                .onFailure { exception ->
                    Log.e("[DriveFolderPicker]", "Failed to create folder", exception)
                    _uiState.update { it.copy(error = exception.message) }
                }
        }
    }
}
