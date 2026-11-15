package com.fraccetta.scopabriscola.ui.game.scopa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.bluetooth.BtManager
import com.fraccetta.scopabriscola.bluetooth.BtMessage
import com.fraccetta.scopabriscola.bluetooth.ConnectionEvent
import com.fraccetta.scopabriscola.data.GameSession
import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck
import com.fraccetta.scopabriscola.game.scopa.ScopaAction
import com.fraccetta.scopabriscola.game.scopa.ScopaGame
import com.fraccetta.scopabriscola.game.scopa.ScopaState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ScopaUiState(
    val state: ScopaState,
    val isServer: Boolean,
    val localPlayer: Int,
    val opponentName: String? = null
)

@HiltViewModel
class ScopaGameViewModel @Inject constructor(
    private val btManager: BtManager
) : ViewModel() {

    private val scopaGame = ScopaGame()
    private val isServer: Boolean = GameSession.isServer
    private val localPlayer: Int = if (isServer) 1 else 2

    private val initialState = scopaGame.newSession()

    private val _uiState = MutableStateFlow(
        ScopaUiState(
            state = initialState,
            isServer = isServer,
            localPlayer = localPlayer,
            opponentName = GameSession.opponentName
        )
    )
    val uiState: StateFlow<ScopaUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private val cardLookup: Map<String, Card> = Deck.full().associateBy { it.assetName }

    private val json = Json { ignoreUnknownKeys = true }

    init {
        if (isServer) {
            // Server sends initial game state to client
            sendGameState(initialState)
        }
        viewModelScope.launch {
            btManager.events.collect { event ->
                when (event) {
                    is ConnectionEvent.MessageReceived -> handleMessage(event.msg)
                    else -> {}
                }
            }
        }
    }

    fun onCardPlayed(card: Card) {
        val current = _uiState.value.state
        if (current.currentPlayer != localPlayer) return
        if (isServer) {
            applyAndBroadcast(ScopaAction.PlayCard(card))
        } else {
            btManager.send(BtMessage.PlayCard(card.assetName))
        }
    }

    fun onCaptureChosen(group: List<Card>) {
        val current = _uiState.value.state
        if (current.currentPlayer != localPlayer) return
        if (isServer) {
            applyAndBroadcast(ScopaAction.ChooseCapture(group))
        } else {
            btManager.send(BtMessage.ChooseCapture(group.map { it.assetName }))
        }
    }

    fun onNextRound() {
        if (isServer) {
            applyAndBroadcast(ScopaAction.NextRound)
        } else {
            btManager.send(BtMessage.NextRound)
        }
    }

    private fun applyAndBroadcast(action: ScopaAction) {
        val newState = scopaGame.apply(_uiState.value.state, action)
        _uiState.update { it.copy(state = newState) }
        sendGameState(newState)
        checkForSessionEnd(newState)
    }

    private fun sendGameState(state: ScopaState) {
        val stateJson = json.encodeToString(ScopaState.serializer(), state)
        btManager.send(BtMessage.GameState(stateJson))
    }

    private fun handleMessage(msg: BtMessage) {
        if (isServer) {
            when (msg) {
                is BtMessage.PlayCard -> {
                    val card = cardLookup[msg.cardAssetName] ?: return
                    applyAndBroadcast(ScopaAction.PlayCard(card))
                }
                is BtMessage.ChooseCapture -> {
                    val group = msg.cardAssetNames.mapNotNull { cardLookup[it] }
                    if (group.isNotEmpty()) applyAndBroadcast(ScopaAction.ChooseCapture(group))
                }
                is BtMessage.NextRound -> applyAndBroadcast(ScopaAction.NextRound)
                else -> {}
            }
        } else {
            if (msg is BtMessage.GameState) {
                val newState = try {
                    json.decodeFromString(ScopaState.serializer(), msg.stateJson)
                } catch (_: Exception) {
                    return
                }
                _uiState.update { it.copy(state = newState) }
                checkForSessionEnd(newState)
            }
        }
    }

    private fun checkForSessionEnd(state: ScopaState) {
        if (state.phase == com.fraccetta.scopabriscola.game.scopa.ScopaPhase.SESSION_END) {
            val winner = if (state.player1SessionScore >= state.player2SessionScore) 1 else 2
            viewModelScope.launch {
                _navigationEvent.emit("result/$winner/scopa")
            }
        }
    }
}
