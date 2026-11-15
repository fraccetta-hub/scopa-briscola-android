package com.fraccetta.scopabriscola.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val PLAYER_NAME_KEY = stringPreferencesKey("player_name")
        private val CARD_BACK_KEY = stringPreferencesKey("card_back_path")
    }

    val playerName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PLAYER_NAME_KEY] ?: ""
    }

    val cardBackPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CARD_BACK_KEY] ?: ""
    }

    suspend fun setPlayerName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PLAYER_NAME_KEY] = name
        }
    }

    suspend fun setCardBackPath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[CARD_BACK_KEY] = path
        }
    }
}
