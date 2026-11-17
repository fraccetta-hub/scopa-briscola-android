package com.fraccetta.scopabriscola.ui.game.briscola

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.bluetooth.BtManager
import com.fraccetta.scopabriscola.bluetooth.BtMessage
import com.fraccetta.scopabriscola.bluetooth.ConnectionEvent
import com.fraccetta.scopabriscola.data.GameSession
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import com.fraccetta.scopabriscola.sound.SoundManager
import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck
import com.fraccetta.scopabriscola.domain.Rank
import com.fraccetta.scopabriscola.domain.Suit
import com.fraccetta.scopabriscola.game.briscola.BriscolaAction
import com.fraccetta.scopabriscola.game.briscola.BriscolaCpuAI
import com.fraccetta.scopabriscola.game.briscola.BriscolaGame
import com.fraccetta.scopabriscola.game.briscola.BriscolaPhase
import com.fraccetta.scopabriscola.game.briscola.BriscolaState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

data class BriscolaChatMessage(val text: String, val isMine: Boolean)

data class BriscolaUiState(
    val state: BriscolaState,
    val isServer: Boolean,
    val localPlayer: Int,
    val localPlayerName: String? = null,
    val opponentName: String? = null,
    val showDisconnectionDialog: Boolean = false,
    val opponentLastCard: Card? = null,
    val arenaName: String? = null,
    val chatMessages: List<BriscolaChatMessage> = emptyList(),
    val showChat: Boolean = false,
    val chatPinned: Boolean = false,
    val isSolo: Boolean = false
)

@HiltViewModel
class BriscolaGameViewModel @Inject constructor(
    private val btManager: BtManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val briscolaGame = BriscolaGame()
    private val isServer: Boolean = GameSession.isServer
    private val localPlayer: Int = GameSession.localPlayer
    private val isSolo: Boolean = GameSession.isSolo

    private val initialState = briscolaGame.newSession()

    private val _uiState = MutableStateFlow(
        BriscolaUiState(
            state = initialState,
            isServer = isServer,
            localPlayer = localPlayer,
            localPlayerName = GameSession.localPlayerName,
            opponentName = GameSession.opponentName,
            isSolo = isSolo
        )
    )
    val uiState: StateFlow<BriscolaUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private val cardLookup: Map<String, Card> = Deck.full().associateBy { it.assetName }
    private val json = Json { ignoreUnknownKeys = true }
    private var eventsJob: Job? = null
    private var chatHideJob: Job? = null
    private var opponentCardJob: Job? = null
    private var autoRetryCount = 0

    init {
        viewModelScope.launch {
            userPreferences.soundEnabled.collect { SoundManager.enabled = it }
        }
        if (isServer && !isSolo) sendGameState(initialState)
        if (!isSolo) subscribeToEvents()
        if (isSolo) scheduleCpuTurn(initialState)
    }

    private fun subscribeToEvents() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            btManager.events.collect { event ->
                when (event) {
                    is ConnectionEvent.MessageReceived -> handleMessage(event.msg)
                    is ConnectionEvent.Lost -> {
                        if (autoRetryCount < 1) {
                            autoRetryCount++
                            if (isServer) btManager.startHosting() else GameSession.lastDevice?.let { btManager.connect(it) }
                            subscribeToEvents()
                        } else {
                            autoRetryCount = 0
                            _uiState.update { it.copy(showDisconnectionDialog = true) }
                        }
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

    fun onDisconnectionRetry() {
        if (isServer) btManager.startHosting() else GameSession.lastDevice?.let { btManager.connect(it) }
        subscribeToEvents()
    }

    fun onDisconnectionAbandon() {
        _uiState.update { it.copy(showDisconnectionDialog = false) }
        viewModelScope.launch { _navigationEvent.emit("home") }
    }

    fun onCardPlayed(card: Card) {
        val current = _uiState.value.state
        if (current.currentPlayer != localPlayer) return
        if (current.phase != BriscolaPhase.WAITING_FIRST && current.phase != BriscolaPhase.WAITING_SECOND) return
        val localHand = if (localPlayer == 1) current.player1Hand else current.player2Hand
        if (card !in localHand) return
        SoundManager.playCard()
        _uiState.update { it.copy(opponentLastCard = null) }
        // Server broadcasts its own card to client so client can display it
        if (isServer && !isSolo) btManager.send(BtMessage.OpponentCard(card.assetName))
        if (isServer || isSolo) {
            if (!isSolo) viewModelScope.launch { delay(700); applyAndBroadcast(BriscolaAction.PlayCard(card)) }
            else applyAndBroadcast(BriscolaAction.PlayCard(card))
        } else btManager.send(BtMessage.PlayCard(card.assetName))
    }

    fun setArena(name: String) {
        _uiState.update { it.copy(arenaName = name) }
        if (!isSolo) btManager.send(BtMessage.SetArena(name))
    }

    fun onNextMatch() {
        val state = _uiState.value.state
        if (state.phase != BriscolaPhase.MATCH_END) return
        if (isServer || isSolo) applyAndBroadcast(BriscolaAction.NextMatch)
        else btManager.send(BtMessage.NextRound)
    }

    fun onNextGame() {
        val state = _uiState.value.state
        if (state.phase != BriscolaPhase.GAME_END) return
        if (isServer || isSolo) applyAndBroadcast(BriscolaAction.NextGame)
        else btManager.send(BtMessage.NextRound)
    }

    private fun applyAndBroadcast(action: BriscolaAction) {
        val prevState = _uiState.value.state
        val prevP1Captured = prevState.player1Captured.size
        val prevP2Captured = prevState.player2Captured.size
        val newState = briscolaGame.apply(prevState, action)
        // Trick completed: detect who won
        if (newState.player1Captured.size > prevP1Captured) {
            if (localPlayer == 1) SoundManager.playCapture() else SoundManager.playOpponentCapture()
        } else if (newState.player2Captured.size > prevP2Captured) {
            if (localPlayer == 2) SoundManager.playCapture() else SoundManager.playOpponentCapture()
        }
        _uiState.update { it.copy(state = newState, opponentLastCard = if (isSolo) null else it.opponentLastCard) }
        if (!isSolo) sendGameState(newState)
        checkForSessionEnd(newState)
        if (isSolo) scheduleCpuTurn(newState)
    }

    private fun scheduleCpuTurn(state: BriscolaState) {
        if (state.currentPlayer != 2) return
        if (state.phase != BriscolaPhase.WAITING_FIRST && state.phase != BriscolaPhase.WAITING_SECOND) return
        viewModelScope.launch {
            delay(600)
            val current = _uiState.value.state
            if (current.currentPlayer != 2) return@launch
            if (current.phase != BriscolaPhase.WAITING_FIRST && current.phase != BriscolaPhase.WAITING_SECOND) return@launch
            val card = BriscolaCpuAI.choosePlay(current)
            _uiState.update { it.copy(opponentLastCard = card) }
            delay(700)
            applyAndBroadcast(BriscolaAction.PlayCard(card))
        }
    }

    private fun sendGameState(state: BriscolaState) {
        val masked = state.copy(
            player1Hand = emptyList(),
            deckCards = List(state.deckCards.size) { Card(Rank.ASSO, Suit.DENARI) }
        )
        btManager.send(BtMessage.GameState(json.encodeToString(BriscolaState.serializer(), masked)))
    }

    private fun handleMessage(msg: BtMessage) {
        if (isServer) {
            val state = _uiState.value.state
            when (msg) {
                is BtMessage.PlayCard -> {
                    val card = cardLookup[msg.cardAssetName] ?: return
                    if (state.phase != BriscolaPhase.WAITING_FIRST && state.phase != BriscolaPhase.WAITING_SECOND) return
                    if (state.currentPlayer != 2 || card !in state.player2Hand) return
                    showOpponentCard(card)
                    viewModelScope.launch { delay(700); applyAndBroadcast(BriscolaAction.PlayCard(card)) }
                }
                is BtMessage.NextRound -> {
                    when (state.phase) {
                        BriscolaPhase.MATCH_END -> applyAndBroadcast(BriscolaAction.NextMatch)
                        BriscolaPhase.GAME_END -> applyAndBroadcast(BriscolaAction.NextGame)
                        else -> {}
                    }
                }
                is BtMessage.Chat -> receiveChat(msg.messageKey, isMine = false)
                else -> {}
            }
        } else {
            when (msg) {
                is BtMessage.GameState -> {
                    val newState = try {
                        json.decodeFromString(BriscolaState.serializer(), msg.stateJson)
                    } catch (_: Exception) { return }
                    val prevState = _uiState.value.state
                    val prevP1Captured = prevState.player1Captured.size
                    val prevP2Captured = prevState.player2Captured.size
                    if (newState.player1Captured.size > prevP1Captured) {
                        if (localPlayer == 1) SoundManager.playCapture() else SoundManager.playOpponentCapture()
                    } else if (newState.player2Captured.size > prevP2Captured) {
                        if (localPlayer == 2) SoundManager.playCapture() else SoundManager.playOpponentCapture()
                    }
                    _uiState.update { it.copy(state = newState) }
                    checkForSessionEnd(newState)
                }
                is BtMessage.SetArena -> _uiState.update { it.copy(arenaName = msg.arenaName) }
                is BtMessage.Chat -> receiveChat(msg.messageKey, isMine = false)
                is BtMessage.OpponentCard -> {
                    val card = cardLookup[msg.cardAssetName] ?: return
                    showOpponentCard(card)
                }
                else -> {}
            }
        }
    }

    private fun showOpponentCard(card: Card) {
        opponentCardJob?.cancel()
        _uiState.update { it.copy(opponentLastCard = card) }
        opponentCardJob = viewModelScope.launch {
            delay(1800)
            _uiState.update { it.copy(opponentLastCard = null) }
        }
    }

    fun sendChat(text: String) {
        btManager.send(BtMessage.Chat(text))
        receiveChat(text, isMine = true)
    }

    private fun receiveChat(text: String, isMine: Boolean) {
        _uiState.update { state ->
            val newMsg = BriscolaChatMessage(text, isMine)
            val alreadyOpen = state.showChat
            state.copy(
                chatMessages = (state.chatMessages + newMsg).takeLast(50),
                showChat = true,
                chatPinned = alreadyOpen && state.chatPinned
            )
        }
        // Auto-hide only if user hasn't pinned chat open
        if (!_uiState.value.chatPinned) {
            chatHideJob?.cancel()
            chatHideJob = viewModelScope.launch {
                delay(8000)
                if (!_uiState.value.chatPinned) {
                    _uiState.update { it.copy(showChat = false) }
                }
            }
        }
    }

    fun toggleChat() {
        _uiState.update {
            val newVisible = !it.showChat
            it.copy(showChat = newVisible, chatPinned = newVisible)
        }
        if (!_uiState.value.showChat) chatHideJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        if (!isSolo) btManager.disconnect()
        GameSession.isServer = false
        GameSession.localPlayer = 1
        GameSession.opponentName = null
        GameSession.lastDevice = null
        GameSession.lastSessionPoints = 0
        GameSession.lastSessionScope = 0
    }

    private fun checkForSessionEnd(state: BriscolaState) {
        if (state.phase == BriscolaPhase.SESSION_END) {
            val absoluteWinner = if (state.player1SessionWins > state.player2SessionWins) 1 else 2
            val relativeWinner = if (absoluteWinner == localPlayer) 1 else 2
            if (relativeWinner == 1) SoundManager.playWin() else SoundManager.playLose()
            GameSession.lastSessionPoints = if (localPlayer == 1) state.player1GamePoints else state.player2GamePoints
            GameSession.lastSessionScope = 0
            viewModelScope.launch { _navigationEvent.emit("result/$relativeWinner/briscola") }
        }
    }
}
