package com.fraccetta.scopabriscola.ui.screens.home

import androidx.lifecycle.ViewModel
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    val playerName: Flow<String> = userPreferences.playerName
}
