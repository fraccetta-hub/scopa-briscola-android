package com.fraccetta.scopabriscola.ui.game.briscola

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.fraccetta.scopabriscola.game.briscola.BriscolaRules
import com.fraccetta.scopabriscola.ui.game.DisconnectionDialog

@Composable
fun BriscolaGameScreen(
    navController: NavHostController,
    viewModel: BriscolaGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            navController.navigate(route) {
                popUpTo("game/briscola") { inclusive = true }
            }
        }
    }

    BriscolaTableContent(
        uiState = uiState,
        onCardPlayed = viewModel::onCardPlayed,
        onNewGame = viewModel::onNewGame
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
    onNewGame: () -> Unit
) {
    val state = uiState.state
    val isMyTurn = state.currentPlayer == uiState.localPlayer
    val localHand = if (uiState.localPlayer == 1) state.player1Hand else state.player2Hand
    val localSessionPoints = if (uiState.localPlayer == 1) state.player1SessionPoints else state.player2SessionPoints
    val opponentSessionPoints = if (uiState.localPlayer == 1) state.player2SessionPoints else state.player1SessionPoints

    // GAME_END dialog
    if (state.phase == BriscolaPhase.GAME_END) {
        val p1GamePoints = state.player1Captured.sumOf { BriscolaRules.cardPoints(it.rank) }
        val p2GamePoints = state.player2Captured.sumOf { BriscolaRules.cardPoints(it.rank) }
        val localGamePoints = if (uiState.localPlayer == 1) p1GamePoints else p2GamePoints
        val opponentGamePoints = if (uiState.localPlayer == 1) p2GamePoints else p1GamePoints
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.briscola_game_end_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.briscola_game_end_scores,
                        localGamePoints,
                        opponentGamePoints
                    )
                )
            },
            confirmButton = {
                Button(onClick = onNewGame) {
                    Text(text = stringResource(R.string.briscola_new_game))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top: score bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.briscola_score_label, localSessionPoints, opponentSessionPoints),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.briscola_deck_remaining, state.deckCards.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = if (isMyTurn)
                    stringResource(R.string.briscola_your_turn)
                else
                    stringResource(R.string.briscola_opponent_turn),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isMyTurn)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center: trump card + trick display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trump card label
            Text(
                text = stringResource(R.string.briscola_trump_label),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Trump card (briscola card visible)
                if (!state.briscolaCardTaken) {
                    BriscolaCardImage(
                        card = state.briscolaCard,
                        modifier = Modifier.size(56.dp, 84.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp, 84.dp)
                            .background(Color.Transparent)
                    )
                }

                // Current trick: first card played
                if (state.phase == BriscolaPhase.WAITING_SECOND) {
                    state.firstCardPlayed?.let { firstCard ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.briscola_trick_played),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            BriscolaCardImage(
                                card = firstCard,
                                modifier = Modifier.size(56.dp, 84.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom: local player's hand
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.briscola_your_hand),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(localHand) { card ->
                    val clickable = isMyTurn &&
                            (state.phase == BriscolaPhase.WAITING_FIRST || state.phase == BriscolaPhase.WAITING_SECOND)
                    Box(
                        modifier = Modifier
                            .size(64.dp, 96.dp)
                            .then(
                                if (clickable)
                                    Modifier
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        .clickable { onCardPlayed(card) }
                                else
                                    Modifier
                            )
                    ) {
                        BriscolaCardImage(card = card, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
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
            } catch (_: Exception) {
                null
            }
        }
    }

    val bmp = bitmap
    val cardDesc = stringResource(R.string.briscola_card_fallback, card.assetName)
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = cardDesc,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.briscola_card_fallback, card.assetName),
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}
