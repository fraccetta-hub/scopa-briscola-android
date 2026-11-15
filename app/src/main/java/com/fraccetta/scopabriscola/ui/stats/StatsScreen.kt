package com.fraccetta.scopabriscola.ui.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.data.db.StatisticsEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavHostController, viewModel: StatsViewModel = hiltViewModel()) {
    val scopaStats by viewModel.scopaStats.collectAsState()
    val briscolaStats by viewModel.briscolaStats.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.stats_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(text = stringResource(R.string.stats_tab_scopa)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(text = stringResource(R.string.stats_tab_briscola)) }
                )
            }

            val currentList = if (selectedTab == 0) scopaStats else briscolaStats
            val showScope = selectedTab == 0

            if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.stats_empty))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(currentList) { stat ->
                        StatsRow(stat = stat, showScope = showScope)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stat: StatisticsEntity, showScope: Boolean) {
    val winPercent = if (stat.sessionsPlayed > 0) stat.sessionsWon * 100 / stat.sessionsPlayed else 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.stats_player_name, stat.playerName),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.stats_sessions, stat.sessionsPlayed, stat.sessionsWon, stat.sessionsLost),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.stats_win_percent, winPercent),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.stats_total_points, stat.totalPoints),
                style = MaterialTheme.typography.bodyMedium
            )
            if (showScope) {
                Text(
                    text = stringResource(R.string.stats_total_scope, stat.totalScope),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
