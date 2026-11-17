package com.fraccetta.scopabriscola.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.BuildConfig
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import com.fraccetta.scopabriscola.data.update.UpdateChecker
import com.fraccetta.scopabriscola.data.update.UpdateInfo
import com.fraccetta.scopabriscola.data.update.UpdateResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckState()
    object UpToDate : UpdateCheckState()
    object Error : UpdateCheckState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    val playerName: Flow<String> = userPreferences.playerName
    val cardBackPath: Flow<String> = userPreferences.cardBackPath
    val cpuName: Flow<String> = userPreferences.cpuName
    val soundEnabled: Flow<Boolean> = userPreferences.soundEnabled
    val scopaTargetScore: Flow<Int> = userPreferences.scopaTargetScore

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState = _updateCheckState.asStateFlow()

    fun setPlayerName(name: String) {
        viewModelScope.launch { userPreferences.setPlayerName(name) }
    }

    fun setCardBackPath(path: String) {
        viewModelScope.launch { userPreferences.setCardBackPath(path) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setSoundEnabled(enabled) }
    }

    fun setScopaTargetScore(score: Int) {
        viewModelScope.launch { userPreferences.setScopaTargetScore(score) }
    }

    fun checkForUpdate() {
        if (_updateCheckState.value is UpdateCheckState.Checking) return
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            _updateCheckState.value = when (val result = UpdateChecker.check(BuildConfig.VERSION_NAME)) {
                is UpdateResult.Available -> UpdateCheckState.UpdateAvailable(result.info)
                is UpdateResult.UpToDate -> UpdateCheckState.UpToDate
                is UpdateResult.NetworkError -> UpdateCheckState.Error
            }
        }
    }

    fun dismissUpdateCheck() {
        _updateCheckState.value = UpdateCheckState.Idle
    }
}
