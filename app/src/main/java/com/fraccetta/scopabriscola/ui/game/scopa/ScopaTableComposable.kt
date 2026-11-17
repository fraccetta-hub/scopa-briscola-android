package com.fraccetta.scopabriscola.ui.game.scopa

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card as CardContainer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.fraccetta.scopabriscola.game.scopa.ScopaPhase
import com.fraccetta.scopabriscola.ui.game.GameResultOverlay
import com.fraccetta.scopabriscola.ui.game.RandomArenaBackground
import com.fraccetta.scopabriscola.ui.game.DisconnectionDialog
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream
import com.fraccetta.scopabriscola.ui.theme.TableGreen

@Composable
fun ScopaGameScreen(
    navController: NavHostController,
    viewModel: ScopaGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            if (route == "home") {
                navController.navigate("home") { popUpTo(0) { inclusive = true } }
            } else {
                navController.navigate(route) { popUpTo("game/scopa") { inclusive = true } }
            }
        }
    }

    ScopaTableContent(
        uiState = uiState,
        onCardPlayed = viewModel::onCardPlayed,
        onCaptureChosen = viewModel::onCaptureChosen,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopaTableContent(
    uiState: ScopaUiState,
    onCardPlayed: (Card) -> Unit,
    onCaptureChosen: (List<Card>) -> Unit,
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
    val localGameScore = if (uiState.localPlayer == 1) state.player1GameScore else state.player2GameScore
    val opponentGameScore = if (uiState.localPlayer == 1) state.player2GameScore else state.player1GameScore
    val localSessionWins = if (uiState.localPlayer == 1) state.player1SessionWins else state.player2SessionWins
    val opponentSessionWins = if (uiState.localPlayer == 1) state.player2SessionWins else state.player1SessionWins
    val localScope = if (uiState.localPlayer == 1) state.player1Scope else state.player2Scope
    val opponentScope = if (uiState.localPlayer == 1) state.player2Scope else state.player1Scope
    val opponentName = uiState.opponentName ?: "Avversario"

    val captureSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    // ── MATCH END overlay ─────────────────────────────────────────────
    if (state.phase == ScopaPhase.MATCH_END) {
        val matchScores = uiState.lastMatchScores
        val localRS = if (uiState.localPlayer == 1) matchScores?.first else matchScores?.second
        val oppRS = if (uiState.localPlayer == 1) matchScores?.second else matchScores?.first
        GameResultOverlay(
            isLocalWin = localGameScore >= opponentGameScore,
            titleText = stringResource(R.string.scopa_match_end_title),
            buttonText = stringResource(R.string.scopa_next_match),
            onContinue = onNextMatch
        ) {
            Text(
                text = stringResource(R.string.scopa_match_end_scores, localGameScore, opponentGameScore),
                color = CardCream
            )
            if (localRS != null && oppRS != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.scopa_round_detail_header),
                    fontWeight = FontWeight.Bold,
                    color = CardCream
                )
                RoundScoreRow(stringResource(R.string.scopa_round_carte), localRS.carte, oppRS.carte)
                RoundScoreRow(stringResource(R.string.scopa_round_denari), localRS.denari, oppRS.denari)
                RoundScoreRow(stringResource(R.string.scopa_round_settebello), localRS.settebello, oppRS.settebello)
                RoundScoreRow(stringResource(R.string.scopa_round_primiera), localRS.primiera, oppRS.primiera)
                RoundScoreRow(stringResource(R.string.scopa_round_scope), localRS.scope, oppRS.scope)
            }
        }
    }

    // ── GAME END overlay ──────────────────────────────────────────────
    if (state.phase == ScopaPhase.GAME_END) {
        val matchScores = uiState.lastMatchScores
        val localRS = if (uiState.localPlayer == 1) matchScores?.first else matchScores?.second
        val oppRS = if (uiState.localPlayer == 1) matchScores?.second else matchScores?.first
        GameResultOverlay(
            isLocalWin = localSessionWins >= opponentSessionWins,
            titleText = stringResource(R.string.scopa_game_end_title),
            buttonText = stringResource(R.string.scopa_next_game),
            onContinue = onNextGame
        ) {
            Text(
                text = stringResource(R.string.scopa_game_end_scores, localGameScore, opponentGameScore),
                color = CardCream
            )
            Text(
                text = stringResource(R.string.scopa_session_wins, localSessionWins, opponentSessionWins),
                color = CardCream
            )
            if (localRS != null && oppRS != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.scopa_round_detail_header),
                    fontWeight = FontWeight.Bold,
                    color = CardCream
                )
                RoundScoreRow(stringResource(R.string.scopa_round_carte), localRS.carte, oppRS.carte)
                RoundScoreRow(stringResource(R.string.scopa_round_denari), localRS.denari, oppRS.denari)
                RoundScoreRow(stringResource(R.string.scopa_round_settebello), localRS.settebello, oppRS.settebello)
                RoundScoreRow(stringResource(R.string.scopa_round_primiera), localRS.primiera, oppRS.primiera)
                RoundScoreRow(stringResource(R.string.scopa_round_scope), localRS.scope, oppRS.scope)
            }
        }
    }

    // ── Capture choice bottom sheet ───────────────────────────────────
    if (state.phase == ScopaPhase.WAITING_CAPTURE_CHOICE && isMyTurn) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = captureSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.scopa_choose_capture),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                state.pendingCaptures.forEachIndexed { index, group ->
                    CardContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onCaptureChosen(group) },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.scopa_capture_option, index + 1),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(70.dp)
                            )
                            group.forEach { card ->
                                CardImage(card = card, modifier = Modifier.size(56.dp, 84.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
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
        val localCaptured = if (uiState.localPlayer == 1) state.player1Captured.size else state.player2Captured.size
        val oppCaptured = if (uiState.localPlayer == 1) state.player2Captured.size else state.player1Captured.size
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
                    text = stringResource(R.string.scopa_score_label, localGameScore, opponentGameScore),
                    style = MaterialTheme.typography.titleSmall,
                    color = CardCream
                )
                Text(
                    text = stringResource(R.string.scopa_scope_live, localScope, opponentScope),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGold
                )
                Text(
                    text = stringResource(R.string.scopa_session_wins, localSessionWins, opponentSessionWins),
                    style = MaterialTheme.typography.labelSmall,
                    color = CardCream.copy(alpha = 0.7f)
                )
                Text(
                    text = "Carte: $localCaptured – $oppCaptured",
                    style = MaterialTheme.typography.labelSmall,
                    color = CardCream.copy(alpha = 0.6f)
                )
            }
            Text(
                text = if (isMyTurn) stringResource(R.string.scopa_your_turn)
                       else stringResource(R.string.scopa_opponent_turn),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isMyTurn) AccentGold else CardCream,
                modifier = if (isMyTurn) Modifier.scale(turnScale) else Modifier
            )
        }

        // ── Opponent hand ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = opponentName,
                style = MaterialTheme.typography.labelMedium,
                color = CardCream,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val count = opponentHandSize.coerceAtLeast(0)
                repeat(count) { i ->
                    BackCardImage(modifier = Modifier.size(44.dp, 66.dp).offset(x = (i * (-6)).dp))
                }
                if (count == 0) Box(modifier = Modifier.size(44.dp, 66.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Opponent last played card ─────────────────────────────────
        val opponentCard = uiState.opponentLastCard
        if (opponentCard != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "$opponentName ha giocato:", style = MaterialTheme.typography.labelSmall, color = CardCream)
                Spacer(modifier = Modifier.height(4.dp))
                CardImage(card = opponentCard, modifier = Modifier.size(72.dp, 108.dp))
            }
        }

        // ── Table + deck ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.deckCards.isNotEmpty()) {
                    BackCardImage(modifier = Modifier.size(72.dp, 108.dp))
                    Text(text = "${state.deckCards.size}", style = MaterialTheme.typography.labelSmall, color = CardCream, modifier = Modifier.padding(top = 2.dp))
                } else {
                    Box(modifier = Modifier.size(72.dp, 108.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.scopa_on_table), style = MaterialTheme.typography.labelSmall, color = CardCream, modifier = Modifier.padding(bottom = 4.dp))
                if (state.tableCards.isEmpty()) {
                    Box(modifier = Modifier.size(72.dp, 108.dp).border(1.dp, CardCream.copy(alpha = 0.4f), RoundedCornerShape(4.dp)))
                } else {
                    val rows = state.tableCards.chunked(4)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        rows.forEach { rowCards ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowCards.forEach { card ->
                                    CardImage(card = card, modifier = Modifier.size(72.dp, 108.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Player hand ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1F33))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.scopa_your_hand), style = MaterialTheme.typography.labelMedium, color = CardCream, modifier = Modifier.padding(bottom = 6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(localHand) { card ->
                    val clickable = isMyTurn && state.phase == ScopaPhase.WAITING_PLAY
                    Box(
                        modifier = Modifier
                            .size(80.dp, 120.dp)
                            .then(if (!isMyTurn) Modifier.alpha(0.5f) else Modifier)
                            .then(
                                if (clickable) Modifier.border(2.dp, AccentGold, RoundedCornerShape(4.dp)).clickable { onCardPlayed(card) }
                                else Modifier
                            )
                    ) {
                        CardImage(card = card, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        } // end Column

        // ── SCOPA! flash overlay ──────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.showScopaFlash,
            enter = fadeIn(tween(150)) + scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.5f),
            exit = fadeOut(tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SCOPA!",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    textAlign = TextAlign.Center
                )
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
                    // Free text input
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
                    // Quick messages
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
private fun RoundScoreRow(label: String, myValue: Int, oppValue: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = "$myValue — $oppValue",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (myValue > oppValue) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun BackCardImage(modifier: Modifier = Modifier) {
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
fun CardImage(card: Card, modifier: Modifier = Modifier) {
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
    val cardDesc = stringResource(R.string.scopa_card_fallback, card.assetName)
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
