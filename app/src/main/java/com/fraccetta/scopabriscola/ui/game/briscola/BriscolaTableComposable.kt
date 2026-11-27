package com.fraccetta.scopabriscola.ui.game.briscola

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
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
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.game.briscola.BriscolaPhase
import com.fraccetta.scopabriscola.ui.game.GameResultOverlay
import com.fraccetta.scopabriscola.ui.game.RandomArenaBackground
import com.fraccetta.scopabriscola.ui.game.DisconnectionDialog
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream
import com.fraccetta.scopabriscola.ui.theme.TableGreen

@Composable
fun BriscolaGameScreen(
    navController: NavHostController,
    viewModel: BriscolaGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            if (route == "home") {
                navController.navigate("home") { popUpTo(0) { inclusive = true } }
            } else {
                navController.navigate(route) { popUpTo("game/briscola") { inclusive = true } }
            }
        }
    }

    BriscolaTableContent(
        uiState = uiState,
        onCardPlayed = viewModel::onCardPlayed,
        onNextMatch = viewModel::onNextMatch,
        onNextGame = viewModel::onNextGame,
        onArenaSelected = { if (uiState.isServer) viewModel.setArena(it) },
        onSendChat = viewModel::sendChat,
        onToggleChat = viewModel::toggleChat,
        onClearTrickResult = viewModel::clearTrickResult
    )

    if (uiState.showDisconnectionDialog) {
        DisconnectionDialog(
            onRetry = { viewModel.onDisconnectionRetry() },
            onAbandon = { viewModel.onDisconnectionAbandon() }
        )
    }
}

@Composable
fun BriscolaTableContent(
    uiState: BriscolaUiState,
    onCardPlayed: (Card) -> Unit,
    onNextMatch: () -> Unit,
    onNextGame: () -> Unit,
    onArenaSelected: (String) -> Unit = {},
    onSendChat: (String) -> Unit = {},
    onToggleChat: () -> Unit = {},
    onClearTrickResult: () -> Unit = {}
) {
    val state = uiState.state
    val isMyTurn = state.currentPlayer == uiState.localPlayer
    val localHand = if (uiState.localPlayer == 1) state.player1Hand else state.player2Hand
    val opponentHandSize = if (uiState.localPlayer == 1) state.player2Hand.size else state.player1Hand.size
    val localGamePoints = if (uiState.localPlayer == 1) state.player1GamePoints else state.player2GamePoints
    val opponentGamePoints = if (uiState.localPlayer == 1) state.player2GamePoints else state.player1GamePoints
    val localSessionWins = if (uiState.localPlayer == 1) state.player1SessionWins else state.player2SessionWins
    val opponentSessionWins = if (uiState.localPlayer == 1) state.player2SessionWins else state.player1SessionWins
    val opponentName = uiState.opponentName ?: "Captain"
    val myName = uiState.localPlayerName ?: "Me"

    // ── MATCH END overlay ──────────────────────────────────────────────
    if (state.phase == BriscolaPhase.MATCH_END) {
        val localMatchPts = if (uiState.localPlayer == 1) state.player1MatchPoints else state.player2MatchPoints
        val oppMatchPts = if (uiState.localPlayer == 1) state.player2MatchPoints else state.player1MatchPoints
        GameResultOverlay(
            isLocalWin = localMatchPts > oppMatchPts,
            isDraw = localMatchPts == oppMatchPts,
            titleText = stringResource(R.string.briscola_match_end_title),
            buttonText = stringResource(R.string.briscola_next_match),
            onContinue = onNextMatch
        ) {
            Text(
                text = "Game: $myName $localGamePoints — $opponentName $opponentGamePoints",
                color = CardCream,
                fontWeight = FontWeight.Bold
            )
            val prevRounds = uiState.matchHistory.dropLast(1)
            if (prevRounds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                prevRounds.forEachIndexed { idx, (myPts, opPts) ->
                    val winner = when {
                        myPts > opPts -> myName
                        opPts > myPts -> opponentName
                        else -> "Draw"
                    }
                    Text(
                        text = "Match ${idx + 1}: $winner ($myPts—$opPts)",
                        style = MaterialTheme.typography.bodySmall,
                        color = CardCream.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Match ${uiState.matchHistory.size}: $myName $localMatchPts — $opponentName $oppMatchPts",
                style = MaterialTheme.typography.bodySmall,
                color = CardCream
            )
        }
    }

    // ── GAME END overlay ───────────────────────────────────────────────
    if (state.phase == BriscolaPhase.GAME_END) {
        GameResultOverlay(
            isLocalWin = localGamePoints > opponentGamePoints,
            isDraw = localGamePoints == opponentGamePoints,
            titleText = stringResource(R.string.briscola_game_end_title),
            buttonText = stringResource(R.string.briscola_next_game),
            onContinue = onNextGame
        ) {
            Text(
                text = "Game: $myName $localGamePoints — $opponentName $opponentGamePoints",
                color = CardCream,
                fontWeight = FontWeight.Bold
            )
            val history = uiState.matchHistory
            if (history.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                history.forEachIndexed { idx, (myPts, opPts) ->
                    val winner = when {
                        myPts > opPts -> myName
                        opPts > myPts -> opponentName
                        else -> "Draw"
                    }
                    Text(
                        text = "Match ${idx + 1}: $winner ($myPts—$opPts)",
                        style = MaterialTheme.typography.bodySmall,
                        color = CardCream.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RandomArenaBackground(
            arenaName = uiState.arenaName,
            overlayAlpha = 0.55f,
            modifier = Modifier.fillMaxSize(),
            onArenaSelected = onArenaSelected
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
        // ── Score bar ────────────────────────────────────────────────
        val myName = uiState.localPlayerName ?: "Me"
        val turnPulse = rememberInfiniteTransition(label = "turn")
        val turnScale by turnPulse.animateFloat(
            initialValue = 1f, targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "scale"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1F33))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // Header row
                Row {
                    Text(text = "        ", style = MaterialTheme.typography.bodySmall, color = Color.Transparent)
                    Text(text = myName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AccentGold, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    Text(text = opponentName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = CardCream, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Points row
                Row {
                    Text(text = "Pts", style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.width(52.dp))
                    Text(text = "$localGamePoints", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    Text(text = "$opponentGamePoints", style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                }
                // Games row
                Row {
                    Text(text = "Games", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.width(52.dp))
                    Text(text = "$localSessionWins", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    Text(text = "$opponentSessionWins", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                }
            }
            Text(
                text = if (isMyTurn) stringResource(R.string.briscola_your_turn)
                       else stringResource(R.string.briscola_opponent_turn),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isMyTurn) AccentGold else CardCream,
                modifier = if (isMyTurn) Modifier.scale(turnScale) else Modifier
            )
        }

        // ── Opponent hand ─────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = opponentName, style = MaterialTheme.typography.labelMedium, color = CardCream, modifier = Modifier.padding(bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                val count = opponentHandSize.coerceAtLeast(0)
                repeat(count) { i ->
                    BriscolaBackCardImage(modifier = Modifier.size(44.dp, 66.dp).offset(x = (i * (-6)).dp))
                }
                if (count == 0) Box(modifier = Modifier.size(44.dp, 66.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Center: briscola trump + deck + trick area ────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            // Briscola trump card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.briscola_trump_label), style = MaterialTheme.typography.labelSmall, color = AccentGold)
                }
                if (!state.briscolaCardTaken) {
                    BriscolaCardImage(card = state.briscolaCard, modifier = Modifier.size(72.dp, 108.dp))
                } else {
                    Box(modifier = Modifier.size(72.dp, 108.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Deck pile
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(16.dp)) // label placeholder
                if (state.deckCards.isNotEmpty()) {
                    BriscolaBackCardImage(modifier = Modifier.size(72.dp, 108.dp))
                    Text(text = "${state.deckCards.size}", style = MaterialTheme.typography.labelSmall, color = CardCream, modifier = Modifier.padding(top = 2.dp))
                } else {
                    Box(modifier = Modifier.size(72.dp, 108.dp))
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Slot 1: first card played
            val opponentCard = uiState.opponentLastCard
            val showOpponentCard = opponentCard != null
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                    if (state.firstCardPlayed != null) {
                        Text(
                            text = if (state.firstPlayerOfTrick == uiState.localPlayer) "Tu" else opponentName,
                            style = MaterialTheme.typography.labelSmall, color = CardCream, fontSize = 9.sp
                        )
                    }
                }
                if (state.firstCardPlayed != null) {
                    BriscolaCardImage(card = state.firstCardPlayed!!, modifier = Modifier.size(72.dp, 108.dp))
                } else {
                    Box(modifier = Modifier.size(72.dp, 108.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Slot 2: opponent card in transit
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                    if (showOpponentCard) {
                        Text(text = opponentName, style = MaterialTheme.typography.labelSmall, color = CardCream, fontSize = 9.sp)
                    }
                }
                if (showOpponentCard && opponentCard != null) {
                    BriscolaCardImage(card = opponentCard, modifier = Modifier.size(72.dp, 108.dp))
                } else {
                    Box(modifier = Modifier.size(72.dp, 108.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Player hand ───────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1F33)).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.briscola_your_hand), style = MaterialTheme.typography.labelMedium, color = CardCream, modifier = Modifier.padding(bottom = 6.dp))
            LazyRow(
                modifier = Modifier.height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(localHand) { card ->
                    val clickable = isMyTurn && (state.phase == BriscolaPhase.WAITING_FIRST || state.phase == BriscolaPhase.WAITING_SECOND)
                    Box(
                        modifier = Modifier
                            .size(80.dp, 120.dp)
                            .then(if (!isMyTurn) Modifier.alpha(0.5f) else Modifier)
                            .then(
                                if (clickable) Modifier.border(2.dp, AccentGold, RoundedCornerShape(4.dp)).clickable { onCardPlayed(card) }
                                else Modifier
                            )
                    ) {
                        BriscolaCardImage(card = card, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        } // end Column

        // ── Trick capture animation ──────────────────────────────────
        val trickAnim = remember { Animatable(0f) }
        var animatingTrick by remember { mutableStateOf<TrickResult?>(null) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        LaunchedEffect(uiState.lastTrickResult) {
            val result = uiState.lastTrickResult ?: return@LaunchedEffect
            animatingTrick = result
            trickAnim.snapTo(0f)
            scope.launch {
                trickAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                onClearTrickResult()
                animatingTrick = null
            }
        }
        val atr = animatingTrick
        if (atr != null) {
            val moveYDp = (trickAnim.value * (if (atr.localWon) 320f else -320f)).dp
            val alpha = (1f - trickAnim.value * 1.4f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .offset(y = moveYDp)
                        .alpha(alpha),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BriscolaCardImage(card = atr.card1, modifier = Modifier.size(72.dp, 108.dp))
                    BriscolaCardImage(card = atr.card2, modifier = Modifier.size(72.dp, 108.dp))
                }
            }
        }

        // ── Chat UI (multiplayer only) ────────────────────────────────
        if (!uiState.isSolo) {
            val predefinedMessages = listOf("Nice move!", "Nooo!", "Good luck!", "Great!", "Too bad...", "Almost there")
            val chatListState = rememberLazyListState()
            var chatInput by remember { mutableStateOf("") }
            LaunchedEffect(uiState.chatMessages.size) {
                if (uiState.chatMessages.isNotEmpty()) {
                    chatListState.animateScrollToItem(uiState.chatMessages.size - 1)
                }
            }

            AnimatedVisibility(
                visible = uiState.showChat,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 72.dp),
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it }
            ) {
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .background(Color(0xDD0D1F33), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (uiState.chatMessages.isNotEmpty()) {
                        LazyColumn(
                            state = chatListState,
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.chatMessages) { msg ->
                                Text(
                                    text = if (msg.isMine) "Io: ${msg.text}" else "${uiState.opponentName ?: "Avv"}: ${msg.text}",
                                    fontSize = 11.sp,
                                    color = if (msg.isMine) AccentGold else CardCream,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Scrivi...", fontSize = 11.sp, color = CardCream.copy(alpha = 0.5f)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CardCream,
                                unfocusedTextColor = CardCream,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = CardCream.copy(alpha = 0.4f)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                                if (chatInput.isNotBlank()) { onSendChat(chatInput.trim()); chatInput = "" }
                            })
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(32.dp)
                                .background(AccentGold, CircleShape)
                                .clickable { if (chatInput.isNotBlank()) { onSendChat(chatInput.trim()); chatInput = "" } },
                            contentAlignment = Alignment.Center
                        ) { Text("▶", fontSize = 12.sp, color = Color.White) }
                    }
                    predefinedMessages.forEach { msg ->
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = CardCream,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                .clickable { onSendChat(msg) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 16.dp)
                    .size(48.dp)
                    .background(AccentGold, CircleShape)
                    .clickable { onToggleChat() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💬", fontSize = 20.sp)
            }
        }
    } // end Box
}

@Composable
fun BriscolaBackCardImage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open("backs/default_back.png").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) { null }
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color(0xFF1A3D8F), RoundedCornerShape(4.dp)).border(1.dp, CardCream.copy(alpha = 0.5f), RoundedCornerShape(4.dp)))
    }
}

@Composable
fun BriscolaCardImage(card: Card, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(card.assetName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(card.assetName) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open("cards/${card.assetName}.png").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) { null }
        }
    }

    val bmp = bitmap
    val cardDesc = stringResource(R.string.briscola_card_fallback, card.assetName)
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = cardDesc, modifier = modifier)
    } else {
        Box(
            modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = card.assetName, fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(2.dp))
        }
    }
}
