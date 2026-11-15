package com.fraccetta.scopabriscola.ui.game.scopa

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card as CardContainer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.fraccetta.scopabriscola.game.scopa.ScopaPhase

@Composable
fun ScopaGameScreen(
    navController: NavHostController,
    viewModel: ScopaGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            navController.navigate(route) {
                popUpTo("game/scopa") { inclusive = true }
            }
        }
    }

    ScopaTableContent(
        uiState = uiState,
        onCardPlayed = viewModel::onCardPlayed,
        onCaptureChosen = viewModel::onCaptureChosen,
        onNextRound = viewModel::onNextRound
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopaTableContent(
    uiState: ScopaUiState,
    onCardPlayed: (Card) -> Unit,
    onCaptureChosen: (List<Card>) -> Unit,
    onNextRound: () -> Unit
) {
    val state = uiState.state
    val isMyTurn = state.currentPlayer == uiState.localPlayer
    val localHand = if (uiState.localPlayer == 1) state.player1Hand else state.player2Hand
    val localScore = if (uiState.localPlayer == 1) state.player1SessionScore else state.player2SessionScore
    val opponentScore = if (uiState.localPlayer == 1) state.player2SessionScore else state.player1SessionScore

    val captureSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Round-end dialog
    if (state.phase == ScopaPhase.ROUND_END) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.scopa_round_end_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.scopa_round_end_scores,
                        localScore,
                        opponentScore
                    )
                )
            },
            confirmButton = {
                Button(onClick = onNextRound) {
                    Text(text = stringResource(R.string.scopa_next_round))
                }
            }
        )
    }

    // Capture choice bottom sheet
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
                                CardImage(card = card, modifier = Modifier.size(48.dp, 72.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
                text = stringResource(R.string.scopa_score_label, localScore, opponentScore),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.scopa_deck_remaining, state.deckCards.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = if (isMyTurn)
                    stringResource(R.string.scopa_your_turn)
                else
                    stringResource(R.string.scopa_opponent_turn),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isMyTurn)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center: table cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.scopa_on_table),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.tableCards) { card ->
                    CardImage(card = card, modifier = Modifier.size(56.dp, 84.dp))
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
                text = stringResource(R.string.scopa_your_hand),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(localHand) { card ->
                    val clickable = isMyTurn && state.phase == ScopaPhase.WAITING_PLAY
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
                        CardImage(card = card, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * Loads a card image from assets/cards/<assetName>.png.
 * Falls back to a text label if the asset is not found.
 */
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
            } catch (_: Exception) {
                null
            }
        }
    }

    val bmp = bitmap
    val cardDesc = stringResource(R.string.scopa_card_fallback, card.assetName)
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
                text = stringResource(R.string.scopa_card_fallback, card.assetName),
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}
