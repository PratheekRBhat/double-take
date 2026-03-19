package com.pratheekbhat.doubletake.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthDataStore @Inject constructor(private val context: Context) {

    private object Keys {
        val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        val ACCOUNT_NAME = stringPreferencesKey("account_name")
    }

    val isSignedIn: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.IS_SIGNED_IN] ?: false }

    val accountName: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.ACCOUNT_NAME] }

    suspend fun saveAuthState(accountName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_SIGNED_IN] = true
            prefs[Keys.ACCOUNT_NAME] = accountName
        }
    }

    suspend fun clearAuthState() {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_SIGNED_IN] = false
            prefs.remove(Keys.ACCOUNT_NAME)
        }
    }
}