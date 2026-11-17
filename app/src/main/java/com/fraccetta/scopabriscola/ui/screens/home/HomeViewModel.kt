package com.fraccetta.scopabriscola.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.BuildConfig
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import com.fraccetta.scopabriscola.data.update.UpdateChecker
import com.fraccetta.scopabriscola.data.update.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    val playerName: Flow<String> = userPreferences.playerName
    val cpuName: Flow<String> = userPreferences.cpuName

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    init {
        viewModelScope.launch {
            _updateInfo.value = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }
}
