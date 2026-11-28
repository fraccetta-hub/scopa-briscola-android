package com.fraccetta.scopabriscola.ui.game.scopone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.game.scopone.ScoponeGame
import com.fraccetta.scopabriscola.game.scopone.ScoponePhase
import com.fraccetta.scopabriscola.ui.game.scopa.BackCardImage
import com.fraccetta.scopabriscola.ui.game.scopa.CardImage
import com.fraccetta.scopabriscola.ui.game.DisconnectionDialog
import com.fraccetta.scopabriscola.ui.game.GameResultOverlay
import com.fraccetta.scopabriscola.ui.game.RandomArenaBackground
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream
import com.fraccetta.scopabriscola.ui.theme.TableGreen

@Composable
fun ScoponeGameScreen(
    navController: NavHostController,
    viewModel: ScoponeGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            if (route == "home") {
                navController.navigate("home") { popUpTo(0) { inclusive = true } }
            } else {
                navController.navigate(route) { popUpTo("game/scopone") { inclusive = true } }
            }
        }
    }

    ScoponeTableContent(
        uiState = uiState,
        onCardPlayed = viewModel::onCardPlayed,
        onCaptureChosen = viewModel::onCaptureChosen,
        onNextMatch = viewModel::onNextMatch,
        onNextGame = viewModel::onNextGame,
        onDisconnectionRetry = viewModel::onDisconnectionRetry,
        onDisconnectionAbandon = viewModel::onDisconnectionAbandon,
        onClearCaptureResult = viewModel::clearCaptureResult
    )
}

@Composable
private fun ScoponeTableContent(
    uiState: ScoponeUiState,
    onCardPlayed: (Card) -> Unit,
    onCaptureChosen: (List<Card>) -> Unit,
    onNextMatch: () -> Unit,
    onNextGame: () -> Unit,
    onDisconnectionRetry: () -> Unit,
    onDisconnectionAbandon: () -> Unit,
    onClearCaptureResult: () -> Unit
) {
    val state = uiState.state
    val localPlayer = uiState.localPlayer
    val partnerPlayer = localPlayer xor 2  // 0↔2, 1↔3
    // CCW: nextPlayer(P) = left, prevPlayer(P) = right
    val leftPlayer = ScoponeGame.nextPlayer(localPlayer)
    val rightPlayer = when (localPlayer) { 0 -> 1; 1 -> 2; 2 -> 3; else -> 0 }

    val myHand = state.hands[localPlayer]
    val displayHand = myHand.sortedBy { it.rank.numericValue }
    val isMyTurn = state.currentPlayer == localPlayer
    val phase = state.phase
    val myTeam = ScoponeGame.teamOf(localPlayer)

    // Deal animation
    var dealtCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.matchId) {
        dealtCount = 0
        repeat(10) { i ->
            delay(80L)
            dealtCount = i + 1
        }
    }

    // Capture replay dialog
    var showReplayDialog by remember { mutableStateOf(false) }

    // Played card animation (slide from player position to center)
    val playCardOffsetX = remember { Animatable(0f) }
    val playCardOffsetY = remember { Animatable(0f) }
    LaunchedEffect(uiState.opponentLastCard) {
        if (uiState.opponentLastCard != null) {
            val startX = when (state.currentPlayer) {
                leftPlayer -> -250f
                rightPlayer -> 250f
                else -> 0f
            }
            val startY = when (state.currentPlayer) {
                partnerPlayer -> -300f
                else -> 0f
            }
            playCardOffsetX.snapTo(startX)
            playCardOffsetY.snapTo(startY)
            launch { playCardOffsetX.animateTo(0f, tween(350)) }
            playCardOffsetY.animateTo(0f, tween(350))
        }
    }

    // Capture overlay animation (cards fly toward capturer)
    var captureOverlayCards by remember { mutableStateOf<List<Card>?>(null) }
    val captureOffsetX = remember { Animatable(0f) }
    val captureOffsetY = remember { Animatable(0f) }
    val captureAlpha = remember { Animatable(0f) }
    LaunchedEffect(state.lastCapture) {
        val cap = state.lastCapture ?: return@LaunchedEffect
        if (cap.cards.size < 2) return@LaunchedEffect
        captureOverlayCards = cap.cards
        val targetX = when (cap.playerWho) {
            leftPlayer -> -280f
            rightPlayer -> 280f
            else -> 0f
        }
        val targetY = when (cap.playerWho) {
            partnerPlayer -> -350f
            localPlayer -> 350f
            else -> 0f
        }
        captureOffsetX.snapTo(0f)
        captureOffsetY.snapTo(0f)
        captureAlpha.snapTo(1f)
        launch { captureOffsetX.animateTo(targetX, tween(500)) }
        launch { captureOffsetY.animateTo(targetY, tween(500)) }
        captureAlpha.animateTo(0f, tween(500))
        captureOverlayCards = null
    }

    // Capture toast
    var captureToast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.lastCapture) {
        val cap = state.lastCapture ?: return@LaunchedEffect
        if (cap.cards.size < 2) return@LaunchedEffect
        val name = when (cap.playerWho) {
            localPlayer -> uiState.localPlayerName ?: "Tu"
            partnerPlayer -> if (uiState.btMode == 1) uiState.partnerName ?: "Partner" else "Captain"
            else -> "AI ${cap.playerWho + 1}"
        }
        val n = cap.cards.size
        captureToast = "$name ha preso $n ${if (n == 1) "carta" else "carte"}"
        delay(1800L)
        captureToast = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RandomArenaBackground()

        // Scopa flash
        AnimatedVisibility(
            visible = uiState.showScopaFlash,
            enter = fadeIn(tween(200)) + scaleIn(),
            exit = fadeOut(tween(500)) + scaleOut(),
            modifier = Modifier.align(Alignment.Center).offset(y = (-60).dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xBBFFD700), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("SCOPA!", color = Color(0xFF3D1A00), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top: partner row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                PlayerSeatTop(
                    playerIndex = partnerPlayer,
                    handSize = state.hands[partnerPlayer].size,
                    capturedCount = state.captured[partnerPlayer].size,
                    scopeCount = state.scope[partnerPlayer],
                    isActive = state.currentPlayer == partnerPlayer,
                    name = if (uiState.btMode == 1) uiState.partnerName ?: "Partner" else "Captain"
                )
            }

            // Middle row: left opponent | table | right opponent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left opponent
                Box(modifier = Modifier.width(72.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                    PlayerSeatSide(
                        playerIndex = leftPlayer,
                        handSize = state.hands[leftPlayer].size,
                        capturedCount = state.captured[leftPlayer].size,
                        scopeCount = state.scope[leftPlayer],
                        isActive = state.currentPlayer == leftPlayer,
                        isLeft = true,
                        name = "AI ${leftPlayer + 1}"
                    )
                }

                // Table center
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Color(0x33000000), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Team scores
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TeamScoreChip(label = "Noi", score = state.teamGameScore[myTeam], highlight = true)
                            TeamScoreChip(label = "Loro", score = state.teamGameScore[1 - myTeam], highlight = false)
                        }

                        // Currently played card by AI/partner
                        if (uiState.opponentLastCard != null) {
                            val playingName = when (state.currentPlayer) {
                                partnerPlayer -> if (uiState.btMode == 1) uiState.partnerName ?: "Partner" else "Captain"
                                else -> "AI ${state.currentPlayer + 1}"
                            }
                            Column(
                                modifier = Modifier.graphicsLayer {
                                    translationX = playCardOffsetX.value
                                    translationY = playCardOffsetY.value
                                },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("$playingName gioca:", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.size(48.dp, 72.dp)) {
                                    CardImage(card = uiState.opponentLastCard, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }

                        // Table cards
                        if (state.tableCards.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp, 108.dp)
                                    .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tavolo\nvuoto", color = CardCream.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        } else {
                            val isChoosingCapture = phase == ScoponePhase.WAITING_CAPTURE_CHOICE && state.currentPlayer == localPlayer
                            val multiGroup = isChoosingCapture && state.pendingCaptures.size > 1
                            val groupColors = listOf(AccentGold, Color(0xFF00BCD4), Color(0xFF66BB6A), Color(0xFFFF7043))
                            if (multiGroup) {
                                Text(
                                    "Scegli quale gruppo prendere",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val chunks = state.tableCards.chunked(5)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                chunks.forEach { rowCards ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        rowCards.forEach { card ->
                                            val groupIndex = if (isChoosingCapture) state.pendingCaptures.indexOfFirst { it.contains(card) } else -1
                                            val inPending = groupIndex >= 0
                                            val borderColor = groupColors.getOrElse(groupIndex) { AccentGold }
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp, 78.dp)
                                                    .then(if (inPending) Modifier.border(3.dp, borderColor, RoundedCornerShape(6.dp)) else Modifier)
                                                    .then(if (inPending) Modifier.clickable {
                                                        val group = state.pendingCaptures.getOrNull(groupIndex)
                                                        if (group != null) onCaptureChosen(group)
                                                    } else Modifier)
                                            ) {
                                                CardImage(card = card, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Turn indicator
                        val currentName = when (state.currentPlayer) {
                            localPlayer -> uiState.localPlayerName ?: "Tu"
                            partnerPlayer -> if (uiState.btMode == 1) uiState.partnerName ?: "Partner" else "Captain"
                            else -> "AI ${state.currentPlayer + 1}"
                        }
                        Text(
                            text = when {
                                phase == ScoponePhase.MATCH_END -> "Fine mano"
                                phase == ScoponePhase.GAME_END -> "Fine partita"
                                phase == ScoponePhase.WAITING_CAPTURE_CHOICE && isMyTurn -> "→ tocca una carta colorata"
                                phase == ScoponePhase.WAITING_CAPTURE_CHOICE -> "Sceglie $currentName…"
                                isMyTurn -> "Tocca a te"
                                else -> "Gioca $currentName…"
                            },
                            color = if (isMyTurn) AccentGold else CardCream.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = if (isMyTurn) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Right opponent
                Box(modifier = Modifier.width(72.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                    PlayerSeatSide(
                        playerIndex = rightPlayer,
                        handSize = state.hands[rightPlayer].size,
                        capturedCount = state.captured[rightPlayer].size,
                        scopeCount = state.scope[rightPlayer],
                        isActive = state.currentPlayer == rightPlayer,
                        isLeft = false,
                        name = "AI ${rightPlayer + 1}"
                    )
                }
            }

            // Bottom: my hand
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Player name + scope
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = uiState.localPlayerName ?: "Tu",
                            color = CardCream,
                            fontSize = 12.sp
                        )
                        if (state.scope[localPlayer] > 0) {
                            repeat(state.scope[localPlayer].coerceAtMost(5)) {
                                Text("⭐", fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = "${state.captured[localPlayer].size} carte",
                            color = CardCream.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy((-28).dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(displayHand) { index, card ->
                            val dealt = index < dealtCount
                            val dealScale by animateFloatAsState(if (dealt) 1f else 0.3f, if (dealt) tween(180) else snap(), label = "")
                            val dealAlpha by animateFloatAsState(if (dealt) 1f else 0f, if (dealt) tween(180) else snap(), label = "")
                            Box(
                                modifier = Modifier
                                    .size(72.dp, 108.dp)
                                    .graphicsLayer {
                                        alpha = dealAlpha * if (!isMyTurn) 0.6f else 1f
                                        scaleX = dealScale; scaleY = dealScale
                                    }
                                    .then(if (isMyTurn && phase == ScoponePhase.WAITING_PLAY)
                                        Modifier.clickable { onCardPlayed(card) } else Modifier)
                            ) {
                                CardImage(card = card, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }

        // Capture overlay animation
        captureOverlayCards?.let { cards ->
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = captureOffsetX.value
                        translationY = captureOffsetY.value
                        alpha = captureAlpha.value
                    },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                cards.take(6).forEach { card ->
                    Box(modifier = Modifier.size(48.dp, 72.dp)) {
                        CardImage(card = card, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        // Capture toast
        captureToast?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .background(Color(0xDD1A2A40), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(msg, color = CardCream, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Replay button (last capture)
        if (uiState.lastCaptureDisplay != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 160.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xBB1A3A5C), CircleShape)
                        .clickable { showReplayDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("👁", fontSize = 18.sp)
                }
            }
        }

        // Phase overlays
        if (phase == ScoponePhase.MATCH_END || phase == ScoponePhase.GAME_END || phase == ScoponePhase.SESSION_END) {
            ScoponeResultOverlay(
                uiState = uiState,
                onNextMatch = onNextMatch,
                onNextGame = onNextGame
            )
        }

        if (uiState.showDisconnectionDialog) {
            DisconnectionDialog(
                onRetry = onDisconnectionRetry,
                onAbandon = onDisconnectionAbandon
            )
        }

        // Capture replay dialog
        if (showReplayDialog && uiState.lastCaptureDisplay != null) {
            val result = uiState.lastCaptureDisplay
            AlertDialog(
                onDismissRequest = { showReplayDialog = false },
                title = { Text(if (result.localWon) "La tua squadra ha preso" else "Avversari hanno preso") },
                text = {
                    val cardSize = if (result.cards.size <= 3) 56.dp to 84.dp else 44.dp to 66.dp
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(result.cards) { _, card ->
                            Box(modifier = Modifier.size(cardSize.first, cardSize.second)) {
                                CardImage(card = card, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showReplayDialog = false }) { Text("OK") } }
            )
        }
    }
}

@Composable
private fun TeamScoreChip(label: String, score: Int, highlight: Boolean) {
    Box(
        modifier = Modifier
            .background(if (highlight) Color(0xFF1A5276) else Color(0xFF2C1A33), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text("$label: $score", color = if (highlight) AccentGold else CardCream, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlayerSeatTop(
    playerIndex: Int,
    handSize: Int,
    capturedCount: Int,
    scopeCount: Int,
    isActive: Boolean,
    name: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-18).dp)) {
            repeat(handSize.coerceAtMost(10)) {
                BackCardImage(modifier = Modifier.size(28.dp, 42.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isActive) {
                Box(modifier = Modifier.size(8.dp).background(AccentGold, CircleShape))
            }
            Text(name, color = if (isActive) AccentGold else CardCream, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
            Text("$capturedCount♠", color = CardCream.copy(alpha = 0.6f), fontSize = 10.sp)
            if (scopeCount > 0) Text("⭐×$scopeCount", fontSize = 10.sp)
        }
    }
}

@Composable
private fun PlayerSeatSide(
    playerIndex: Int,
    handSize: Int,
    capturedCount: Int,
    scopeCount: Int,
    isActive: Boolean,
    isLeft: Boolean,
    name: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        if (isActive) {
            Box(modifier = Modifier.size(8.dp).background(AccentGold, CircleShape))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy((-24).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(handSize.coerceAtMost(8)) {
                BackCardImage(modifier = Modifier.size(28.dp, 42.dp).rotate(90f))
            }
        }
        Text(
            text = "$capturedCount♠",
            color = if (isActive) AccentGold else CardCream.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        if (scopeCount > 0) Text("⭐×$scopeCount", fontSize = 10.sp)
    }
}

@Composable
private fun ScoponeResultOverlay(
    uiState: ScoponeUiState,
    onNextMatch: () -> Unit,
    onNextGame: () -> Unit
) {
    val state = uiState.state
    val myTeam = ScoponeGame.teamOf(uiState.localPlayer)
    val myScore = state.teamGameScore[myTeam]
    val oppScore = state.teamGameScore[1 - myTeam]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFF1A2A40), RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.phase) {
                ScoponePhase.MATCH_END -> {
                    Text("Fine Mano", color = AccentGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    uiState.lastMatchScores?.let { (s0, s1) ->
                        val myMatchScore = if (myTeam == 0) s0 else s1
                        val oppMatchScore = if (myTeam == 0) s1 else s0
                        Text("Noi: +${myMatchScore.total}pt  Loro: +${oppMatchScore.total}pt", color = CardCream, fontSize = 14.sp)
                        ScoponeScoreDetail(myMatchScore, oppMatchScore)
                    }
                    Text("Totale — Noi: $myScore  Loro: $oppScore", color = CardCream, fontSize = 13.sp)
                    Button(onClick = onNextMatch, colors = ButtonDefaults.buttonColors(containerColor = AccentGold)) {
                        Text("Prossima mano", color = Color(0xFF0D1F33), fontWeight = FontWeight.Bold)
                    }
                }
                ScoponePhase.GAME_END -> {
                    val weWon = myScore > oppScore
                    Text(if (weWon) "Avete vinto! 🏆" else "Avete perso", color = if (weWon) AccentGold else CardCream, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Noi: $myScore  Loro: $oppScore", color = CardCream, fontSize = 15.sp)
                    Button(onClick = onNextGame, colors = ButtonDefaults.buttonColors(containerColor = AccentGold)) {
                        Text("Nuova partita", color = Color(0xFF0D1F33), fontWeight = FontWeight.Bold)
                    }
                }
                ScoponePhase.SESSION_END -> {
                    val weWon = state.teamSessionWins[myTeam] > state.teamSessionWins[1 - myTeam]
                    Text(if (weWon) "Sessione vinta! 🏆" else "Sessione persa", color = if (weWon) AccentGold else CardCream, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Wins — Noi: ${state.teamSessionWins[myTeam]}  Loro: ${state.teamSessionWins[1 - myTeam]}", color = CardCream, fontSize = 14.sp)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ScoponeScoreDetail(
    myScore: com.fraccetta.scopabriscola.game.scopa.RoundScore,
    oppScore: com.fraccetta.scopabriscola.game.scopa.RoundScore
) {
    val items = listOf(
        Triple("Carte", myScore.carte, oppScore.carte),
        Triple("Denari", myScore.denari, oppScore.denari),
        Triple("Settebello", myScore.settebello, oppScore.settebello),
        Triple("Primiera", myScore.primiera, oppScore.primiera),
        Triple("Scope", myScore.scope, oppScore.scope)
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEach { (label, mine, theirs) ->
            val isCount = label == "Scope"
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$label: ", color = CardCream.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.width(70.dp))
                Text(if (mine > 0) (if (isCount) "$mine" else "✓") else "—", color = if (mine > 0) Color(0xFF4CAF50) else CardCream.copy(alpha = 0.4f), fontSize = 11.sp)
                Text(" / ", color = CardCream.copy(alpha = 0.4f), fontSize = 11.sp)
                Text(if (theirs > 0) (if (isCount) "$theirs" else "✓") else "—", color = if (theirs > 0) Color(0xFFF44336) else CardCream.copy(alpha = 0.4f), fontSize = 11.sp)
            }
        }
    }
}
