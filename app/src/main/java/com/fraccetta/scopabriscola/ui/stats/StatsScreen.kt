package com.fraccetta.scopabriscola.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.data.db.GameResultEntity
import com.fraccetta.scopabriscola.data.db.StatisticsEntity
import com.fraccetta.scopabriscola.ui.game.AssetBackground
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavHostController, viewModel: StatsViewModel = hiltViewModel()) {
    val scopaStats by viewModel.scopaStats.collectAsState()
    val briscolaStats by viewModel.briscolaStats.collectAsState()
    val allResults by viewModel.allResults.collectAsState()
    val opponents by viewModel.opponents.collectAsState()
    val maxConsecutiveScope by viewModel.maxConsecutiveScope.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1F33), Color(0xFF1A3A5C), Color(0xFF0D2D4A)))))
        AssetBackground(assetPath = "backgrounds/menu_bg.webp", overlayAlpha = 0.45f, modifier = Modifier.fillMaxSize())
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.stats_title), color = CardCream) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.stats_back),
                                tint = CardCream
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text(text = stringResource(R.string.stats_tab_scopa)) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text(text = stringResource(R.string.stats_tab_briscola)) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        text = { Text(text = "Opponents") })
                }

                when (selectedTab) {
                    0 -> ScopaStatsTab(scopaStats, allResults.filter { it.gameType == "scopa" }, maxConsecutiveScope)
                    1 -> GameStatsTab(briscolaStats, allResults.filter { it.gameType == "briscola" }, showScope = false)
                    2 -> OpponentsTab(opponents, allResults)
                }
            }
        }
    }
}

@Composable
private fun ScopaStatsTab(stats: List<StatisticsEntity>, results: List<GameResultEntity>, maxConsecutiveScope: Int) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (maxConsecutiveScope > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A40))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏆", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Consecutive scope record: $maxConsecutiveScope", color = AccentGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (results.isNotEmpty()) {
            item {
                WinLossChart(results = results.takeLast(20))
            }
            item {
                PointsHistoryChart(results = results.takeLast(20))
            }
        }
        if (stats.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.stats_empty), color = CardCream)
                }
            }
        } else {
            items(stats) { stat ->
                StatsRow(stat = stat, showScope = true)
            }
        }
    }
}

@Composable
private fun GameStatsTab(stats: List<StatisticsEntity>, results: List<GameResultEntity>, showScope: Boolean) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (results.isNotEmpty()) {
            item {
                WinLossChart(results = results.takeLast(20))
            }
            item {
                PointsHistoryChart(results = results.takeLast(20))
            }
        }
        if (stats.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.stats_empty), color = CardCream)
                }
            }
        } else {
            items(stats) { stat ->
                StatsRow(stat = stat, showScope = showScope)
            }
        }
    }
}

@Composable
private fun OpponentsTab(opponents: List<String>, allResults: List<GameResultEntity>) {
    if (opponents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No games against opponents.", color = CardCream)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(opponents) { opponent ->
                val results = allResults.filter { it.opponentName == opponent && !it.isSolo }
                val wins = results.count { it.won }
                val losses = results.size - wins
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A40)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = opponent, style = MaterialTheme.typography.titleMedium, color = AccentGold)
                        Text(text = "Games: ${results.size}  •  W: $wins  •  L: $losses", color = CardCream)
                        if (results.isNotEmpty()) {
                            val pct = wins * 100 / results.size
                            Text(text = "Win rate: $pct%", color = CardCream.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            WinLossChart(results = results.takeLast(10))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WinLossChart(results: List<GameResultEntity>) {
    val winColor = Color(0xFF4CAF50)
    val lossColor = Color(0xFFF44336)
    val barWidth = 12f
    val barSpacing = 4f

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = "Recent games", fontSize = 11.sp, color = CardCream.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val totalWidth = results.size * (barWidth + barSpacing)
            val startX = (size.width - totalWidth) / 2f
            results.forEachIndexed { i, result ->
                val x = startX + i * (barWidth + barSpacing)
                drawRoundRect(
                    color = if (result.won) winColor else lossColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(3f)
                )
            }
        }
        Row(modifier = Modifier.padding(start = 4.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(modifier = Modifier.width(10.dp).height(10.dp)) {
                    drawRoundRect(color = winColor, cornerRadius = CornerRadius(2f))
                }
                Text(text = "V", fontSize = 10.sp, color = CardCream.copy(alpha = 0.7f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(modifier = Modifier.width(10.dp).height(10.dp)) {
                    drawRoundRect(color = lossColor, cornerRadius = CornerRadius(2f))
                }
                Text(text = "S", fontSize = 10.sp, color = CardCream.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun PointsHistoryChart(results: List<GameResultEntity>) {
    if (results.size < 2) return
    val winColor = Color(0xFF4CAF50)
    val lossColor = Color(0xFFF44336)
    val lineColor = Color(0xFFD4AF37).copy(alpha = 0.6f)
    val maxPts = results.maxOf { it.points }.coerceAtLeast(1)
    val minPts = results.minOf { it.points }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = "Punti per partita", fontSize = 11.sp, color = CardCream.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
            val w = size.width
            val h = size.height
            val range = (maxPts - minPts).toFloat().coerceAtLeast(1f)
            val stepX = w / (results.size - 1).coerceAtLeast(1)

            // area fill
            val path = Path()
            results.forEachIndexed { i, r ->
                val x = i * stepX
                val y = h - ((r.points - minPts) / range) * (h * 0.85f) - h * 0.05f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.lineTo((results.size - 1) * stepX, h)
            path.lineTo(0f, h)
            path.close()
            drawPath(path, color = lineColor.copy(alpha = 0.15f))

            // line
            results.forEachIndexed { i, r ->
                if (i == 0) return@forEachIndexed
                val x0 = (i - 1) * stepX
                val y0 = h - ((results[i - 1].points - minPts) / range) * (h * 0.85f) - h * 0.05f
                val x1 = i * stepX
                val y1 = h - ((r.points - minPts) / range) * (h * 0.85f) - h * 0.05f
                drawLine(color = lineColor, start = Offset(x0, y0), end = Offset(x1, y1), strokeWidth = 2f)
            }

            // dots
            results.forEachIndexed { i, r ->
                val x = i * stepX
                val y = h - ((r.points - minPts) / range) * (h * 0.85f) - h * 0.05f
                drawCircle(color = if (r.won) winColor else lossColor, radius = 5f, center = Offset(x, y))
            }
        }
        Row(modifier = Modifier.padding(start = 4.dp, top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "min $minPts  max $maxPts pts", fontSize = 10.sp, color = CardCream.copy(alpha = 0.5f))
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
            val modeTag = if (stat.isSolo) stringResource(R.string.stats_mode_cpu)
                          else stringResource(R.string.stats_mode_bt)
            Text(
                text = "${stat.playerName} · $modeTag",
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
