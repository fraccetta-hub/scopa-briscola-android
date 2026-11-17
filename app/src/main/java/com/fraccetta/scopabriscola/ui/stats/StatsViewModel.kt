package com.fraccetta.scopabriscola.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.data.db.GameResultEntity
import com.fraccetta.scopabriscola.data.db.StatisticsEntity
import com.fraccetta.scopabriscola.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val allStats = statsRepository.getAllStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scopaStats: StateFlow<List<StatisticsEntity>> = allStats
        .map { list -> list.filter { it.gameType == "scopa" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val briscolaStats: StateFlow<List<StatisticsEntity>> = allStats
        .map { list -> list.filter { it.gameType == "briscola" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResults: StateFlow<List<GameResultEntity>> = statsRepository.getAllResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val opponents: StateFlow<List<String>> = statsRepository.getOpponents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _maxConsecutiveScope = MutableStateFlow(0)
    val maxConsecutiveScope: StateFlow<Int> = _maxConsecutiveScope.asStateFlow()

    init {
        viewModelScope.launch {
            _maxConsecutiveScope.value = statsRepository.getMaxConsecutiveScope()
        }
    }
}
