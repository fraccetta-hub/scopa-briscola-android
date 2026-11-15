package com.fraccetta.scopabriscola.ui.game.scopa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.bluetooth.BtManager
import com.fraccetta.scopabriscola.bluetooth.BtMessage
import com.fraccetta.scopabriscola.bluetooth.ConnectionEvent
import com.fraccetta.scopabriscola.data.GameSession
import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck
import com.fraccetta.scopabriscola.domain.Rank
import com.fraccetta.scopabriscola.domain.Suit
import com.fraccetta.scopabriscola.game.scopa.ScopaAction
import com.fraccetta.scopabriscola.game.scopa.ScopaGame
import com.fraccetta.scopabriscola.game.scopa.ScopaPhase
import com.fraccetta.scopabriscola.game.scopa.ScopaState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val opponentName: String? = null,
    val showDisconnectionDialog: Boolean = false
)

@HiltViewModel
class ScopaGameViewModel @Inject constructor(
    private val btManager: BtManager
) : ViewModel() {

    private val scopaGame = ScopaGame()
    private val isServer: Boolean = GameSession.isServer
    private val localPlayer: Int = GameSession.localPlayer

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

    private val _navigationEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private val cardLookup: Map<String, Card> = Deck.full().associateBy { it.assetName }

    private val json = Json { ignoreUnknownKeys = true }

    private var eventsJob: Job? = null

    private fun subscribeToEvents() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            btManager.events.collect { event ->
                when (event) {
                    is ConnectionEvent.MessageReceived -> handleMessage(event.msg)
                    is ConnectionEvent.Lost -> {
                        _uiState.update { it.copy(showDisconnectionDialog = true) }
                    }
                    is ConnectionEvent.Connected -> {
                        _uiState.update { it.copy(showDisconnectionDialog = false) }
                        if (isServer) sendGameState(_uiState.value.state)
                    }
                    else -> {}
                }
            }
        }
    }

    init {
        if (isServer) {
            // Server sends initial game state to client
            sendGameState(initialState)
        }
        subscribeToEvents()
    }

    fun onDisconnectionRetry() {
        if (isServer) {
            btManager.startHosting()
        } else {
            GameSession.lastDevice?.let { btManager.connect(it) }
        }
        subscribeToEvents()
    }

    fun onDisconnectionAbandon() {
        _uiState.update { it.copy(showDisconnectionDialog = false) }
        viewModelScope.launch {
            _navigationEvent.emit("home")
        }
    }

    fun onCardPlayed(card: Card) {
        val current = _uiState.value.state
        if (current.currentPlayer != localPlayer) return
        if (current.phase != ScopaPhase.WAITING_PLAY) return
        val localHand = if (localPlayer == 1) current.player1Hand else current.player2Hand
        if (card !in localHand) return
        if (isServer) {
            applyAndBroadcast(ScopaAction.PlayCard(card))
        } else {
            btManager.send(BtMessage.PlayCard(card.assetName))
        }
    }

    fun onCaptureChosen(group: List<Card>) {
        val current = _uiState.value.state
        if (current.currentPlayer != localPlayer) return
        if (current.phase != ScopaPhase.WAITING_CAPTURE_CHOICE) return
        if (group !in current.pendingCaptures) return
        if (isServer) {
            applyAndBroadcast(ScopaAction.ChooseCapture(group))
        } else {
            btManager.send(BtMessage.ChooseCapture(group.map { it.assetName }))
        }
    }

    fun onNextRound() {
        val state = _uiState.value.state
        if (state.phase != ScopaPhase.ROUND_END) return
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
        // Mask server's hand and deck contents before sending to client
        val masked = state.copy(
            player1Hand = emptyList(),
            deckCards = List(state.deckCards.size) { Card(Rank.ASSO, Suit.DENARI) }
        )
        val stateJson = json.encodeToString(ScopaState.serializer(), masked)
        btManager.send(BtMessage.GameState(stateJson))
    }

    private fun handleMessage(msg: BtMessage) {
        if (isServer) {
            val state = _uiState.value.state
            when (msg) {
                is BtMessage.PlayCard -> {
                    val card = cardLookup[msg.cardAssetName] ?: return
                    if (state.phase != ScopaPhase.WAITING_PLAY || state.currentPlayer != 2 || card !in state.player2Hand) return
                    applyAndBroadcast(ScopaAction.PlayCard(card))
                }
                is BtMessage.ChooseCapture -> {
                    if (state.phase != ScopaPhase.WAITING_CAPTURE_CHOICE || state.currentPlayer != 2) return
                    val group = msg.cardAssetNames.mapNotNull { cardLookup[it] }
                    if (group !in state.pendingCaptures) return
                    applyAndBroadcast(ScopaAction.ChooseCapture(group))
                }
                is BtMessage.NextRound -> {
                    if (state.phase != ScopaPhase.ROUND_END) return
                    applyAndBroadcast(ScopaAction.NextRound)
                }
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

    override fun onCleared() {
        super.onCleared()
        btManager.disconnect()
        GameSession.isServer = false
        GameSession.localPlayer = 1
        GameSession.opponentName = null
        GameSession.lastDevice = null
        GameSession.lastSessionPoints = 0
        GameSession.lastSessionScope = 0
    }

    private fun checkForSessionEnd(state: ScopaState) {
        if (state.phase == ScopaPhase.SESSION_END) {
            val absoluteWinner = if (state.player1SessionScore >= state.player2SessionScore) 1 else 2
            val relativeWinner = if (absoluteWinner == localPlayer) 1 else 2
            GameSession.lastSessionPoints = if (localPlayer == 1) state.player1SessionScore else state.player2SessionScore
            GameSession.lastSessionScope = if (localPlayer == 1) state.player1Scope else state.player2Scope
            viewModelScope.launch {
                _navigationEvent.emit("result/$relativeWinner/scopa")
            }
        }
    }
}
