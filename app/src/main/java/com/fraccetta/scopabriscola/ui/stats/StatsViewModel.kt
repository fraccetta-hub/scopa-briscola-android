package com.fraccetta.scopabriscola.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.data.db.StatisticsEntity
import com.fraccetta.scopabriscola.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
}
