package com.fraccetta.scopabriscola.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val PLAYER_NAME_KEY = stringPreferencesKey("player_name")
        private val CARD_BACK_KEY = stringPreferencesKey("card_back_path")
        private val CPU_NAME_KEY = stringPreferencesKey("cpu_name")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        private val SCOPA_TARGET_SCORE_KEY = intPreferencesKey("scopa_target_score")
    }

    val playerName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PLAYER_NAME_KEY] ?: ""
    }

    val cardBackPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CARD_BACK_KEY] ?: ""
    }

    val cpuName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CPU_NAME_KEY] ?: "Il Capitano"
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SOUND_ENABLED_KEY] ?: true
    }

    val scopaTargetScore: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SCOPA_TARGET_SCORE_KEY] ?: 11
    }

    suspend fun setPlayerName(name: String) {
        context.dataStore.edit { prefs -> prefs[PLAYER_NAME_KEY] = name }
    }

    suspend fun setCardBackPath(path: String) {
        context.dataStore.edit { prefs -> prefs[CARD_BACK_KEY] = path }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[SOUND_ENABLED_KEY] = enabled }
    }

    suspend fun setScopaTargetScore(score: Int) {
        context.dataStore.edit { prefs -> prefs[SCOPA_TARGET_SCORE_KEY] = score }
    }
}
