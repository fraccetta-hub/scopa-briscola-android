package com.fraccetta.scopabriscola.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    val playerName: Flow<String> = userPreferences.playerName
    val cardBackPath: Flow<String> = userPreferences.cardBackPath

    fun setPlayerName(name: String) {
        viewModelScope.launch {
            userPreferences.setPlayerName(name)
        }
    }

    fun setCardBackPath(path: String) {
        viewModelScope.launch {
            userPreferences.setCardBackPath(path)
        }
    }
}
