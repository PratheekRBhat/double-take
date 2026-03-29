package com.pratheekbhat.doubletake.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratheekbhat.doubletake.data.local.AuthDataStore
import com.pratheekbhat.doubletake.domain.repository.AuthRepository
import com.pratheekbhat.doubletake.worker.SyncWorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val accountEmail: String? = null,
    val signedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authDataStore: AuthDataStore,
    private val syncWorkManager: SyncWorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadAccountEmail()
    }

    private fun loadAccountEmail() {
        viewModelScope.launch {
            authDataStore.accountName.collect { email ->
                _uiState.update { it.copy(accountEmail = email) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            syncWorkManager.cancelAllSync()
            authRepository.signOut()
            Log.d("[SettingsViewModel]", "Signed out and cancelled all syncs")
            _uiState.update { it.copy(signedOut = true) }
        }
    }
}
