package com.fraccetta.scopabriscola.ui.game.scopa

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
import com.fraccetta.scopabriscola.game.scopa.RoundScore
import com.fraccetta.scopabriscola.game.scopa.ScopaAction
import com.fraccetta.scopabriscola.game.scopa.ScopaCpuAI
import com.fraccetta.scopabriscola.game.scopa.ScopaGame
import com.fraccetta.scopabriscola.game.scopa.ScopaPhase
import com.fraccetta.scopabriscola.game.scopa.ScopaScorer
import com.fraccetta.scopabriscola.game.scopa.ScopaState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ChatMessage(val text: String, val isMine: Boolean)

data class ScopaUiState(
    val state: ScopaState,
    val isServer: Boolean,
    val localPlayer: Int,
    val opponentName: String? = null,
    val showDisconnectionDialog: Boolean = false,
    val cpuLastCard: Card? = null,
    val lastMatchScores: Pair<RoundScore, RoundScore>? = null,
    val arenaName: String? = null,
    val showScopaFlash: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val showChat: Boolean = false,
    val isSolo: Boolean = false
)

@HiltViewModel
class ScopaGameViewModel @Inject constructor(
    private val btManager: BtManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val scopaGame = ScopaGame()
    private val isServer: Boolean = GameSession.isServer
    private val localPlayer: Int = GameSession.localPlayer
    private val isSolo: Boolean = GameSession.isSolo

    private var initialState: ScopaState = ScopaState(
        tableCards = emptyList(), player1Hand = emptyList(), player2Hand = emptyList(),
        player1Captured = emptyList(), player2Captured = emptyList(),
        player1Scope = 0, player2Scope = 0, deckCards = emptyList(),
        currentPlayer = 1, phase = ScopaPhase.WAITING_PLAY, pendingCaptures = emptyList()
    )

    private val _uiState = MutableStateFlow(
        ScopaUiState(state = initialState, isServer = isServer, localPlayer = localPlayer,
            opponentName = GameSession.opponentName, isSolo = isSolo)
    )
    val uiState: StateFlow<ScopaUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private val cardLookup: Map<String, Card> = Deck.full().associateBy { it.assetName }
    private val json = Json { ignoreUnknownKeys = true }
    private var eventsJob: Job? = null
    private var chatHideJob: Job? = null
    private var autoRetryCount = 0
    private var consecutiveScope = 0
    private var maxConsecutiveScope = 0

    init {
        viewModelScope.launch {
            userPreferences.soundEnabled.collect { SoundManager.enabled = it }
        }
        viewModelScope.launch {
            val targetScore = userPreferences.scopaTargetScore.first()
            initialState = scopaGame.newSession(targetScore)
            _uiState.update { it.copy(state = initialState) }
            if (isServer && !isSolo) sendGameState(initialState)
            if (!isSolo) subscribeToEvents()
        }
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
        if (current.phase != ScopaPhase.WAITING_PLAY) return
        val localHand = if (localPlayer == 1) current.player1Hand else current.player2Hand
        if (card !in localHand) return
        SoundManager.playCard()
        _uiState.update { it.copy(cpuLastCard = null) }
        if (isServer || isSolo) applyAndBroadcast(ScopaAction.PlayCard(card))
        else btManager.send(BtMessage.PlayCard(card.assetName))
    }

    fun onCaptureChosen(group: List<Card>) {
        val current = _uiState.value.state
        if (current.currentPlayer != localPlayer) return
        if (current.phase != ScopaPhase.WAITING_CAPTURE_CHOICE) return
        if (group !in current.pendingCaptures) return
        SoundManager.playCapture()
        if (isServer) applyAndBroadcast(ScopaAction.ChooseCapture(group))
        else btManager.send(BtMessage.ChooseCapture(group.map { it.assetName }))
    }

    fun setArena(name: String) {
        _uiState.update { it.copy(arenaName = name) }
        if (!isSolo) btManager.send(BtMessage.SetArena(name))
    }

    fun onNextMatch() {
        val state = _uiState.value.state
        if (state.phase != ScopaPhase.MATCH_END) return
        if (isServer || isSolo) applyAndBroadcast(ScopaAction.NextMatch)
        else btManager.send(BtMessage.NextRound)
    }

    fun onNextGame() {
        val state = _uiState.value.state
        if (state.phase != ScopaPhase.GAME_END) return
        if (isServer || isSolo) applyAndBroadcast(ScopaAction.NextGame)
        else btManager.send(BtMessage.NextRound)
    }

    private fun applyAndBroadcast(action: ScopaAction) {
        val prevState = _uiState.value.state
        val prevScopeTotal = prevState.player1Scope + prevState.player2Scope
        val prevP1Captured = prevState.player1Captured.size
        val prevP2Captured = prevState.player2Captured.size
        val newState = scopaGame.apply(prevState, action)
        val newScopeTotal = newState.player1Scope + newState.player2Scope
        if (newScopeTotal > prevScopeTotal) {
            SoundManager.playScopa()
            triggerScopaFlash()
            consecutiveScope++
            if (consecutiveScope > maxConsecutiveScope) maxConsecutiveScope = consecutiveScope
        } else if (action is ScopaAction.PlayCard && newState.phase != ScopaPhase.WAITING_CAPTURE_CHOICE) {
            consecutiveScope = 0
        }
        // Auto-capture (single match, no capture-choice dialog)
        if (action is ScopaAction.PlayCard && newState.phase != ScopaPhase.WAITING_CAPTURE_CHOICE) {
            val localCaptured = if (localPlayer == 1) newState.player1Captured.size else newState.player2Captured.size
            val prevLocalCaptured = if (localPlayer == 1) prevP1Captured else prevP2Captured
            val oppCaptured = if (localPlayer == 1) newState.player2Captured.size else newState.player1Captured.size
            val prevOppCaptured = if (localPlayer == 1) prevP2Captured else prevP1Captured
            when {
                localCaptured > prevLocalCaptured -> SoundManager.playCapture()
                oppCaptured > prevOppCaptured -> SoundManager.playOpponentCapture()
            }
        }
        // CPU ChooseCapture
        if (action is ScopaAction.ChooseCapture) {
            val prevCpu = if (localPlayer == 1) prevP2Captured else prevP1Captured
            val newCpu = if (localPlayer == 1) newState.player2Captured.size else newState.player1Captured.size
            if (newCpu > prevCpu) SoundManager.playOpponentCapture()
        }

        val matchScores = when (action) {
            is ScopaAction.PlayCard, is ScopaAction.ChooseCapture -> {
                if (newState.phase == ScopaPhase.MATCH_END || newState.phase == ScopaPhase.GAME_END) {
                    ScopaScorer.scoreRound(
                        newState.player1Captured, newState.player1Scope,
                        newState.player2Captured, newState.player2Scope
                    )
                } else null
            }
            else -> null
        }

        val clearCpuCard = action is ScopaAction.NextMatch || action is ScopaAction.NextGame
        _uiState.update { prev ->
            prev.copy(
                state = newState,
                lastMatchScores = matchScores ?: prev.lastMatchScores,
                cpuLastCard = if (clearCpuCard) null else prev.cpuLastCard
            )
        }
        if (!isSolo) sendGameState(newState)
        checkForSessionEnd(newState)
        if (isSolo) scheduleCpuTurn(newState)
    }

    private fun scheduleCpuTurn(state: ScopaState) {
        if (state.currentPlayer != 2) return
        viewModelScope.launch {
            delay(600)
            val current = _uiState.value.state
            when (current.phase) {
                ScopaPhase.WAITING_PLAY -> {
                    val card = ScopaCpuAI.choosePlay(current)
                    _uiState.update { it.copy(cpuLastCard = card) }
                    delay(700)
                    applyAndBroadcast(ScopaAction.PlayCard(card))
                }
                ScopaPhase.WAITING_CAPTURE_CHOICE -> {
                    val group = ScopaCpuAI.chooseCapture(current)
                    if (group.isEmpty()) return@launch
                    delay(400)
                    applyAndBroadcast(ScopaAction.ChooseCapture(group))
                }
                else -> {}
            }
        }
    }

    private fun sendGameState(state: ScopaState) {
        val masked = state.copy(
            player1Hand = emptyList(),
            deckCards = List(state.deckCards.size) { Card(Rank.ASSO, Suit.DENARI) }
        )
        btManager.send(BtMessage.GameState(json.encodeToString(ScopaState.serializer(), masked)))
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
                    when (state.phase) {
                        ScopaPhase.MATCH_END -> applyAndBroadcast(ScopaAction.NextMatch)
                        ScopaPhase.GAME_END -> applyAndBroadcast(ScopaAction.NextGame)
                        else -> {}
                    }
                }
                else -> {}
            }
        } else {
            when (msg) {
                is BtMessage.GameState -> {
                    val newState = try {
                        json.decodeFromString(ScopaState.serializer(), msg.stateJson)
                    } catch (_: Exception) { return }
                    val prevState = _uiState.value.state
                    val prevP1Captured = prevState.player1Captured.size
                    val prevP2Captured = prevState.player2Captured.size
                    val prevScopeTotal = prevState.player1Scope + prevState.player2Scope
                    if (newState.player1Scope + newState.player2Scope > prevScopeTotal) {
                        SoundManager.playScopa()
                        triggerScopaFlash()
                    }
                    val localCaptured = if (localPlayer == 1) newState.player1Captured.size else newState.player2Captured.size
                    val prevLocalCaptured = if (localPlayer == 1) prevP1Captured else prevP2Captured
                    val oppCaptured = if (localPlayer == 1) newState.player2Captured.size else newState.player1Captured.size
                    val prevOppCaptured = if (localPlayer == 1) prevP2Captured else prevP1Captured
                    when {
                        localCaptured > prevLocalCaptured -> SoundManager.playCapture()
                        oppCaptured > prevOppCaptured -> SoundManager.playOpponentCapture()
                    }
                    _uiState.update { it.copy(state = newState) }
                    checkForSessionEnd(newState)
                }
                is BtMessage.SetArena -> _uiState.update { it.copy(arenaName = msg.arenaName) }
                is BtMessage.Chat -> receiveChat(msg.messageKey, isMine = false)
                else -> {}
            }
        }
    }

    fun sendChat(messageKey: String) {
        btManager.send(BtMessage.Chat(messageKey))
        receiveChat(messageKey, isMine = true)
    }

    private fun receiveChat(messageKey: String, isMine: Boolean) {
        _uiState.update { state ->
            val newMsg = ChatMessage(messageKey, isMine)
            state.copy(
                chatMessages = (state.chatMessages + newMsg).takeLast(20),
                showChat = true
            )
        }
        chatHideJob?.cancel()
        chatHideJob = viewModelScope.launch {
            delay(4000)
            _uiState.update { it.copy(showChat = false) }
        }
    }

    fun toggleChat() = _uiState.update { it.copy(showChat = !it.showChat) }

    fun triggerScopaFlash() {
        viewModelScope.launch {
            _uiState.update { it.copy(showScopaFlash = true) }
            delay(1600)
            _uiState.update { it.copy(showScopaFlash = false) }
        }
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

    private fun checkForSessionEnd(state: ScopaState) {
        if (state.phase == ScopaPhase.SESSION_END) {
            val absoluteWinner = if (state.player1SessionWins >= state.player2SessionWins) 1 else 2
            val relativeWinner = if (absoluteWinner == localPlayer) 1 else 2
            if (relativeWinner == 1) SoundManager.playWin() else SoundManager.playLose()
            GameSession.lastSessionPoints = if (localPlayer == 1) state.player1GameScore else state.player2GameScore
            GameSession.lastSessionScope = if (localPlayer == 1) state.player1Scope else state.player2Scope
            GameSession.lastMaxConsecutiveScope = maxConsecutiveScope
            viewModelScope.launch { _navigationEvent.emit("result/$relativeWinner/scopa") }
        }
    }
}
