package com.example.biblioscan.session
// UserSessionManager.kt

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userDataStore by preferencesDataStore(name = "user_session")

class UserSessionManager(private val context: Context) {

    companion object {
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    // Sauvegarder le nom d'utilisateur
    suspend fun saveUsername(username: String) {
        context.userDataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    // Lire le nom d'utilisateur
    fun getUsername(): Flow<String?> {
        return context.userDataStore.data.map { preferences ->
            preferences[USERNAME_KEY]
        }
    }

    // Supprimer le nom d'utilisateur (déconnexion)
    suspend fun clearUsername() {
        context.userDataStore.edit { preferences ->
            preferences.remove(USERNAME_KEY)
        }
    }
}
