package com.fraccetta.scopabriscola.ui.game.briscola

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
import com.fraccetta.scopabriscola.game.briscola.BriscolaGame
import com.fraccetta.scopabriscola.game.briscola.BriscolaPhase
import com.fraccetta.scopabriscola.game.briscola.BriscolaState
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

data class BriscolaUiState(
    val state: BriscolaState,
    val isServer: Boolean,
    val localPlayer: Int,
    val opponentName: String? = null,
    val showDisconnectionDialog: Boolean = false
)

@HiltViewModel
class BriscolaGameViewModel @Inject constructor(
    private val btManager: BtManager
) : ViewModel() {

    private val briscolaGame = BriscolaGame(winPoints = 121)
    private val isServer: Boolean = GameSession.isServer
    private val localPlayer: Int = GameSession.localPlayer

    private val initialState = briscolaGame.newSession()

    private val _uiState = MutableStateFlow(
        BriscolaUiState(
            state = initialState,
            isServer = isServer,
            localPlayer = localPlayer,
            opponentName = GameSession.opponentName
        )
    )
    val uiState: StateFlow<BriscolaUiState> = _uiState.asStateFlow()

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
        if (current.phase != BriscolaPhase.WAITING_FIRST && current.phase != BriscolaPhase.WAITING_SECOND) return
        val localHand = if (localPlayer == 1) current.player1Hand else current.player2Hand
        if (card !in localHand) return
        if (isServer) {
            applyAndBroadcast(card)
        } else {
            btManager.send(BtMessage.PlayCard(card.assetName))
        }
    }

    private fun applyAndBroadcast(card: Card) {
        val newState = briscolaGame.apply(_uiState.value.state, card)
        _uiState.update { it.copy(state = newState) }
        sendGameState(newState)
        checkForSessionEnd(newState)
    }

    private fun sendGameState(state: BriscolaState) {
        val masked = state.copy(
            player1Hand = emptyList(),
            deckCards = List(state.deckCards.size) { Card(Rank.ASSO, Suit.DENARI) }
        )
        val stateJson = json.encodeToString(BriscolaState.serializer(), masked)
        btManager.send(BtMessage.GameState(stateJson))
    }

    private fun handleMessage(msg: BtMessage) {
        if (isServer) {
            val state = _uiState.value.state
            when (msg) {
                is BtMessage.PlayCard -> {
                    val card = cardLookup[msg.cardAssetName] ?: return
                    if (state.phase != BriscolaPhase.WAITING_FIRST && state.phase != BriscolaPhase.WAITING_SECOND) return
                    if (state.currentPlayer != 2) return
                    if (card !in state.player2Hand) return
                    applyAndBroadcast(card)
                }
                is BtMessage.NextRound -> {
                    if (state.phase != BriscolaPhase.GAME_END) return
                    val newState = briscolaGame.newGame(state)
                    _uiState.update { it.copy(state = newState) }
                    sendGameState(newState)
                    checkForSessionEnd(newState)
                }
                else -> {}
            }
        } else {
            if (msg is BtMessage.GameState) {
                val newState = try {
                    json.decodeFromString(BriscolaState.serializer(), msg.stateJson)
                } catch (_: Exception) {
                    return
                }
                _uiState.update { it.copy(state = newState) }
                checkForSessionEnd(newState)
            }
        }
    }

    fun onNewGame() {
        val state = _uiState.value.state
        if (state.phase != BriscolaPhase.GAME_END) return
        if (isServer) {
            val newState = briscolaGame.newGame(state)
            _uiState.update { it.copy(state = newState) }
            sendGameState(newState)
            checkForSessionEnd(newState)
        } else {
            btManager.send(BtMessage.NextRound)
        }
    }

    override fun onCleared() {
        super.onCleared()
        GameSession.isServer = false
        GameSession.localPlayer = 1
        GameSession.opponentName = null
    }

    private fun checkForSessionEnd(state: BriscolaState) {
        if (state.phase == BriscolaPhase.SESSION_END) {
            val absoluteWinner = if (state.player1SessionPoints > state.player2SessionPoints) 1 else 2
            val relativeWinner = if (absoluteWinner == localPlayer) 1 else 2
            viewModelScope.launch {
                _navigationEvent.emit("result/$relativeWinner/briscola")
            }
        }
    }
}
