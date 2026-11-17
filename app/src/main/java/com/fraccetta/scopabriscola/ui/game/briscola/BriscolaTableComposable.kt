package com.fraccetta.scopabriscola.ui.game.briscola

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
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
        onToggleChat = viewModel::toggleChat
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
    onToggleChat: () -> Unit = {}
) {
    val state = uiState.state
    val isMyTurn = state.currentPlayer == uiState.localPlayer
    val localHand = if (uiState.localPlayer == 1) state.player1Hand else state.player2Hand
    val opponentHandSize = if (uiState.localPlayer == 1) state.player2Hand.size else state.player1Hand.size
    val localGamePoints = if (uiState.localPlayer == 1) state.player1GamePoints else state.player2GamePoints
    val opponentGamePoints = if (uiState.localPlayer == 1) state.player2GamePoints else state.player1GamePoints
    val localSessionWins = if (uiState.localPlayer == 1) state.player1SessionWins else state.player2SessionWins
    val opponentSessionWins = if (uiState.localPlayer == 1) state.player2SessionWins else state.player1SessionWins
    val opponentName = uiState.opponentName ?: "Avversario"

    // ── MATCH END overlay ──────────────────────────────────────────────
    if (state.phase == BriscolaPhase.MATCH_END) {
        val localMatchPts = if (uiState.localPlayer == 1) state.player1MatchPoints else state.player2MatchPoints
        val oppMatchPts = if (uiState.localPlayer == 1) state.player2MatchPoints else state.player1MatchPoints
        GameResultOverlay(
            isLocalWin = localGamePoints >= opponentGamePoints,
            titleText = stringResource(R.string.briscola_match_end_title),
            buttonText = stringResource(R.string.briscola_next_match),
            onContinue = onNextMatch
        ) {
            Text(
                text = stringResource(R.string.briscola_match_end_scores, localMatchPts, oppMatchPts),
                color = CardCream
            )
            Text(
                text = stringResource(R.string.briscola_score_label, localGamePoints, opponentGamePoints),
                color = CardCream
            )
        }
    }

    // ── GAME END overlay ───────────────────────────────────────────────
    if (state.phase == BriscolaPhase.GAME_END) {
        GameResultOverlay(
            isLocalWin = localSessionWins >= opponentSessionWins,
            titleText = stringResource(R.string.briscola_game_end_title),
            buttonText = stringResource(R.string.briscola_next_game),
            onContinue = onNextGame
        ) {
            Text(
                text = stringResource(R.string.briscola_game_end_scores, localGamePoints, opponentGamePoints),
                color = CardCream
            )
            Text(
                text = stringResource(R.string.briscola_session_wins, localSessionWins, opponentSessionWins),
                color = CardCream
            )
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.briscola_score_label, localGamePoints, opponentGamePoints),
                    style = MaterialTheme.typography.titleSmall,
                    color = CardCream
                )
                Text(
                    text = stringResource(R.string.briscola_session_wins, localSessionWins, opponentSessionWins),
                    style = MaterialTheme.typography.labelSmall,
                    color = CardCream.copy(alpha = 0.7f)
                )
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Briscola trump card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.briscola_trump_label), style = MaterialTheme.typography.labelSmall, color = AccentGold, modifier = Modifier.padding(bottom = 2.dp))
                if (!state.briscolaCardTaken) {
                    BriscolaCardImage(card = state.briscolaCard, modifier = Modifier.size(72.dp, 108.dp))
                } else {
                    Box(modifier = Modifier.size(72.dp, 108.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Deck pile
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.deckCards.isNotEmpty()) {
                    BriscolaBackCardImage(modifier = Modifier.size(72.dp, 108.dp))
                    Text(text = "${state.deckCards.size}", style = MaterialTheme.typography.labelSmall, color = CardCream, modifier = Modifier.padding(top = 2.dp))
                } else {
                    Box(modifier = Modifier.size(72.dp, 108.dp))
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Trick area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "In gioco", style = MaterialTheme.typography.labelSmall, color = CardCream, modifier = Modifier.padding(bottom = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    state.firstCardPlayed?.let { firstCard ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (state.firstPlayerOfTrick == uiState.localPlayer) "Tu" else opponentName,
                                style = MaterialTheme.typography.labelSmall, color = CardCream, fontSize = 9.sp
                            )
                            BriscolaCardImage(card = firstCard, modifier = Modifier.size(72.dp, 108.dp))
                        }
                    } ?: Box(modifier = Modifier.size(72.dp, 108.dp))

                    val cpuCard = uiState.cpuLastCard
                    if (cpuCard != null && state.phase == BriscolaPhase.WAITING_SECOND) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = opponentName, style = MaterialTheme.typography.labelSmall, color = CardCream, fontSize = 9.sp)
                            BriscolaCardImage(card = cpuCard, modifier = Modifier.size(72.dp, 108.dp))
                        }
                    } else if (state.firstCardPlayed == null) {
                        Box(modifier = Modifier.size(72.dp, 108.dp))
                    }
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // ── Chat UI (multiplayer only) ────────────────────────────────
        if (!uiState.isSolo) {
            val predefinedMessages = listOf(
                "Bella mossa!", "Nooo!", "Ci siamo quasi",
                "Buona fortuna!", "Grande!", "Peccato..."
            )
            val chatListState = rememberLazyListState()
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
                        .width(220.dp)
                        .background(Color(0xDD0D1F33), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (uiState.chatMessages.isNotEmpty()) {
                        LazyColumn(
                            state = chatListState,
                            modifier = Modifier.fillMaxWidth().height(80.dp),
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
                        Spacer(modifier = Modifier.height(4.dp))
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
