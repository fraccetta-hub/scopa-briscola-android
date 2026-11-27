package com.fraccetta.scopabriscola.ui.game.scopone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.bluetooth.BtManager
import com.fraccetta.scopabriscola.bluetooth.BtMessage
import com.fraccetta.scopabriscola.bluetooth.ConnectionEvent
import com.fraccetta.scopabriscola.data.GameSession
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import com.fraccetta.scopabriscola.data.repository.StatsRepository
import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck
import com.fraccetta.scopabriscola.game.scopone.ScoponeAction
import com.fraccetta.scopabriscola.game.scopone.ScoponeGame
import com.fraccetta.scopabriscola.game.scopone.ScoponePhase
import com.fraccetta.scopabriscola.game.scopone.ScoponeState
import com.fraccetta.scopabriscola.game.scopone.ScoponeCpuAI
import com.fraccetta.scopabriscola.game.scopa.ScopaScorer
import com.fraccetta.scopabriscola.sound.SoundManager
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

data class ScoponeCaptureResult(val cards: List<Card>, val localWon: Boolean)

data class ScoponeUiState(
    val state: ScoponeState,
    val localPlayer: Int,              // which of 0-3 is the local human
    val btMode: Int,                   // 0=offline, 1=humans+AI, 2=user+AI vs BT+AI
    val isServer: Boolean,
    val localPlayerName: String? = null,
    val opponentName: String? = null,
    val partnerName: String? = null,
    val showDisconnectionDialog: Boolean = false,
    val lastMatchScores: Pair<com.fraccetta.scopabriscola.game.scopa.RoundScore, com.fraccetta.scopabriscola.game.scopa.RoundScore>? = null,
    val showScopaFlash: Boolean = false,
    val lastCaptureResult: ScoponeCaptureResult? = null,
    val lastCaptureDisplay: ScoponeCaptureResult? = null,
    val matchId: Int = 0,
    val opponentLastCard: Card? = null
)

@HiltViewModel
class ScoponeGameViewModel @Inject constructor(
    private val btManager: BtManager,
    private val userPreferences: UserPreferences,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val scoponeGame = ScoponeGame()
    private val localPlayer: Int = GameSession.scoponeLocalPlayer
    private val btMode: Int = GameSession.scoponeBtMode
    private val isServer: Boolean = GameSession.isServer
    private val isSolo: Boolean = GameSession.isSolo
    private var cardPlayPending = false

    private val emptyState = ScoponeState(
        tableCards = emptyList(),
        hands = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
        captured = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
        scope = listOf(0, 0, 0, 0),
        currentPlayer = 0,
        phase = ScoponePhase.WAITING_PLAY,
        pendingCaptures = emptyList()
    )

    private val _uiState = MutableStateFlow(
        ScoponeUiState(
            state = emptyState,
            localPlayer = localPlayer,
            btMode = btMode,
            isServer = isServer,
            localPlayerName = GameSession.localPlayerName,
            opponentName = GameSession.opponentName,
            partnerName = GameSession.partnerName
        )
    )

    val uiState: StateFlow<ScoponeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private val cardLookup: Map<String, Card> = Deck.full().associateBy { it.assetName }
    private val json = Json { ignoreUnknownKeys = true }
    private var eventsJob: Job? = null
    private var opponentCardJob: Job? = null
    private var autoRetryCount = 0

    init {
        viewModelScope.launch {
            userPreferences.soundEnabled.collect { SoundManager.enabled = it }
        }
        viewModelScope.launch {
            val targetScore = userPreferences.scopaTargetScore.first()
            val initialState = scoponeGame.newSession(targetScore)
            _uiState.update { it.copy(state = initialState) }
            if (isServer && !isSolo) sendGameState(initialState)
            if (!isSolo) subscribeToEvents()
            if (isSolo) scheduleCpuTurns(initialState)
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
        if (cardPlayPending) return
        val current = _uiState.value.state
        if (current.currentPlayer != localPlayer) return
        if (current.phase != ScoponePhase.WAITING_PLAY) return
        if (card !in current.hands[localPlayer]) return
        cardPlayPending = true
        SoundManager.playCard()
        _uiState.update { it.copy(opponentLastCard = null) }
        if (isServer && !isSolo) btManager.send(BtMessage.OpponentCard(card.assetName))
        if (isServer || isSolo) {
            if (!isSolo) viewModelScope.launch { delay(700); applyAndBroadcast(ScoponeAction.PlayCard(card)) }
            else applyAndBroadcast(ScoponeAction.PlayCard(card))
        } else btManager.send(BtMessage.PlayCard(card.assetName))
    }

    fun onCaptureChosen(group: List<Card>) {
        val current = _uiState.value.state
        if (current.currentPlayer != localPlayer) return
        if (current.phase != ScoponePhase.WAITING_CAPTURE_CHOICE) return
        if (group !in current.pendingCaptures) return
        SoundManager.playCapture()
        if (isServer) applyAndBroadcast(ScoponeAction.ChooseCapture(group))
        else btManager.send(BtMessage.ChooseCapture(group.map { it.assetName }))
    }

    fun onNextMatch() {
        val state = _uiState.value.state
        if (state.phase != ScoponePhase.MATCH_END) return
        if (isServer || isSolo) applyAndBroadcast(ScoponeAction.NextMatch)
        else btManager.send(BtMessage.NextRound)
    }

    fun onNextGame() {
        val state = _uiState.value.state
        if (state.phase != ScoponePhase.GAME_END) return
        if (isServer || isSolo) applyAndBroadcast(ScoponeAction.NextGame)
        else btManager.send(BtMessage.NextRound)
    }

    private fun applyAndBroadcast(action: ScoponeAction) {
        cardPlayPending = false
        val prevState = _uiState.value.state
        val prevTeamScope = prevState.scope[0] + prevState.scope[2]
        val newState = scoponeGame.apply(prevState, action)
        val newTeamScope = newState.scope[0] + newState.scope[2]
        val myTeam = ScoponeGame.teamOf(localPlayer)

        if (newState.scope.sum() > prevState.scope.sum()) {
            SoundManager.playScopa()
            triggerScopaFlash()
        }

        if (action is ScoponeAction.PlayCard && newState.phase != ScoponePhase.WAITING_CAPTURE_CHOICE) {
            val prevMyCaptured = prevState.captured[localPlayer].size
            val newMyCaptured = newState.captured[localPlayer].size
            val prevMyTeamPartner = if (localPlayer < 2) prevState.captured[localPlayer + 2].size else prevState.captured[localPlayer - 2].size
            val newMyTeamPartner = if (localPlayer < 2) newState.captured[localPlayer + 2].size else newState.captured[localPlayer - 2].size
            val myTeamGrew = newMyCaptured > prevMyCaptured || newMyTeamPartner > prevMyTeamPartner
            val opponentTeamGrew = !myTeamGrew && newState.captured.flatten().size > prevState.captured.flatten().size
            when {
                myTeamGrew -> SoundManager.playCapture()
                opponentTeamGrew -> SoundManager.playOpponentCapture()
            }
        }

        val matchScores = when (action) {
            is ScoponeAction.PlayCard, is ScoponeAction.ChooseCapture -> {
                if (newState.phase == ScoponePhase.MATCH_END || newState.phase == ScoponePhase.GAME_END) {
                    val team0Captured = newState.captured[0] + newState.captured[2]
                    val team0Scope = newState.scope[0] + newState.scope[2]
                    val team1Captured = newState.captured[1] + newState.captured[3]
                    val team1Scope = newState.scope[1] + newState.scope[3]
                    ScopaScorer.scoreRound(team0Captured, team0Scope, team1Captured, team1Scope)
                } else null
            }
            else -> null
        }

        val captureResult = newState.lastCapture?.let { capture ->
            ScoponeCaptureResult(cards = capture.cards, localWon = ScoponeGame.teamOf(capture.playerWho) == myTeam)
        }
        val clearDisplay = action is ScoponeAction.NextMatch || action is ScoponeAction.NextGame
        val isNewDeal = prevState.hands[0].isEmpty() && newState.hands[0].isNotEmpty()

        _uiState.update { prev ->
            prev.copy(
                state = newState,
                lastMatchScores = matchScores ?: prev.lastMatchScores,
                lastCaptureResult = captureResult ?: prev.lastCaptureResult,
                lastCaptureDisplay = if (clearDisplay) null else captureResult ?: prev.lastCaptureDisplay,
                matchId = if (isNewDeal) prev.matchId + 1 else prev.matchId
            )
        }
        if (!isSolo) sendGameState(newState)
        checkForSessionEnd(newState)
        if (isSolo) scheduleCpuTurns(newState)
    }

    private fun scheduleCpuTurns(state: ScoponeState) {
        if (state.currentPlayer == localPlayer) return
        if (state.phase != ScoponePhase.WAITING_PLAY && state.phase != ScoponePhase.WAITING_CAPTURE_CHOICE) return
        viewModelScope.launch {
            delay(600)
            val current = _uiState.value.state
            val cp = current.currentPlayer
            if (cp == localPlayer) return@launch
            when (current.phase) {
                ScoponePhase.WAITING_PLAY -> {
                    val card = ScoponeCpuAI.choosePlay(current, cp)
                    _uiState.update { it.copy(opponentLastCard = card) }
                    delay(700)
                    applyAndBroadcast(ScoponeAction.PlayCard(card))
                }
                ScoponePhase.WAITING_CAPTURE_CHOICE -> {
                    val group = ScoponeCpuAI.chooseCapture(current, cp)
                    if (group.isEmpty()) return@launch
                    delay(400)
                    applyAndBroadcast(ScoponeAction.ChooseCapture(group))
                }
                else -> {}
            }
        }
    }

    private fun sendGameState(state: ScoponeState) {
        // Mask all hands except the client's hand
        val clientPlayer = when (btMode) {
            1 -> 2  // mode 1: client = player 2
            2 -> 1  // mode 2: client = player 1
            else -> 2
        }
        val maskedHands = state.hands.mapIndexed { idx, hand ->
            if (idx == clientPlayer) hand else emptyList()
        }
        val masked = state.copy(hands = maskedHands)
        btManager.send(BtMessage.GameState(json.encodeToString(ScoponeState.serializer(), masked)))
    }

    private fun handleMessage(msg: BtMessage) {
        if (isServer) {
            val state = _uiState.value.state
            val clientPlayer = if (btMode == 2) 1 else 2
            when (msg) {
                is BtMessage.PlayCard -> {
                    val card = cardLookup[msg.cardAssetName] ?: return
                    if (state.phase != ScoponePhase.WAITING_PLAY || state.currentPlayer != clientPlayer) return
                    if (card !in state.hands[clientPlayer]) return
                    showOpponentCard(card)
                    viewModelScope.launch { delay(700); applyAndBroadcast(ScoponeAction.PlayCard(card)) }
                }
                is BtMessage.ChooseCapture -> {
                    if (state.phase != ScoponePhase.WAITING_CAPTURE_CHOICE || state.currentPlayer != clientPlayer) return
                    val group = msg.cardAssetNames.mapNotNull { cardLookup[it] }
                    if (group !in state.pendingCaptures) return
                    applyAndBroadcast(ScoponeAction.ChooseCapture(group))
                }
                is BtMessage.NextRound -> {
                    when (state.phase) {
                        ScoponePhase.MATCH_END -> applyAndBroadcast(ScoponeAction.NextMatch)
                        ScoponePhase.GAME_END -> applyAndBroadcast(ScoponeAction.NextGame)
                        else -> {}
                    }
                }
                else -> {}
            }
        } else {
            when (msg) {
                is BtMessage.GameState -> {
                    cardPlayPending = false
                    val newState = try {
                        json.decodeFromString(ScoponeState.serializer(), msg.stateJson)
                    } catch (_: Exception) { return }
                    // Restore our hand from previous state (server masks it)
                    val prevHand = _uiState.value.state.hands[localPlayer]
                    val restoredHands = newState.hands.toMutableList()
                    // Server sends our hand correctly (or empty if not changed); use prev if new is empty
                    if (restoredHands[localPlayer].isEmpty() && prevHand.isNotEmpty() &&
                        newState.phase == ScoponePhase.WAITING_PLAY &&
                        _uiState.value.state.phase != ScoponePhase.MATCH_END &&
                        _uiState.value.state.phase != ScoponePhase.GAME_END) {
                        // keep prevHand only if same match (cards count same or fewer)
                        restoredHands[localPlayer] = prevHand
                    }
                    val finalState = newState.copy(hands = restoredHands)

                    val prevState = _uiState.value.state
                    val myTeam = ScoponeGame.teamOf(localPlayer)
                    if (newState.scope.sum() > prevState.scope.sum()) {
                        SoundManager.playScopa(); triggerScopaFlash()
                    }
                    val prevMyTeamCaptured = (prevState.captured[0] + prevState.captured[2]).size
                    val newMyTeamCaptured = (newState.captured[0] + newState.captured[2]).size
                    val prevOppTeamCaptured = (prevState.captured[1] + prevState.captured[3]).size
                    val newOppTeamCaptured = (newState.captured[1] + newState.captured[3]).size
                    val myTeamIsTeam0 = myTeam == 0
                    when {
                        myTeamIsTeam0 && newMyTeamCaptured > prevMyTeamCaptured -> SoundManager.playCapture()
                        !myTeamIsTeam0 && newOppTeamCaptured > prevOppTeamCaptured -> SoundManager.playCapture()
                        myTeamIsTeam0 && newOppTeamCaptured > prevOppTeamCaptured -> SoundManager.playOpponentCapture()
                        !myTeamIsTeam0 && newMyTeamCaptured > prevMyTeamCaptured -> SoundManager.playOpponentCapture()
                    }
                    val matchScores = if (newState.phase == ScoponePhase.MATCH_END || newState.phase == ScoponePhase.GAME_END) {
                        val t0c = newState.captured[0] + newState.captured[2]
                        val t1c = newState.captured[1] + newState.captured[3]
                        ScopaScorer.scoreRound(t0c, newState.scope[0] + newState.scope[2], t1c, newState.scope[1] + newState.scope[3])
                    } else null
                    val captureResult = newState.lastCapture?.let { capture ->
                        ScoponeCaptureResult(cards = capture.cards, localWon = ScoponeGame.teamOf(capture.playerWho) == myTeam)
                    }
                    val isNewDeal = prevState.hands[localPlayer].isNotEmpty() && finalState.hands[localPlayer].size == 10
                    _uiState.update { prev ->
                        prev.copy(
                            state = finalState,
                            lastMatchScores = matchScores ?: prev.lastMatchScores,
                            lastCaptureResult = captureResult ?: prev.lastCaptureResult,
                            lastCaptureDisplay = captureResult ?: prev.lastCaptureDisplay,
                            matchId = if (isNewDeal) prev.matchId + 1 else prev.matchId
                        )
                    }
                    checkForSessionEnd(newState)
                }
                is BtMessage.OpponentCard -> {
                    val card = cardLookup[msg.cardAssetName] ?: return
                    showOpponentCard(card)
                }
                else -> {}
            }
        }
    }

    fun clearCaptureResult() {
        _uiState.update { it.copy(lastCaptureResult = null) }
    }

    private fun showOpponentCard(card: Card) {
        opponentCardJob?.cancel()
        _uiState.update { it.copy(opponentLastCard = card) }
        opponentCardJob = viewModelScope.launch {
            delay(1800)
            _uiState.update { it.copy(opponentLastCard = null) }
        }
    }

    fun triggerScopaFlash() {
        viewModelScope.launch {
            _uiState.update { it.copy(showScopaFlash = true) }
            delay(1600)
            _uiState.update { it.copy(showScopaFlash = false) }
        }
    }

    private fun checkForSessionEnd(state: ScoponeState) {
        if (state.phase == ScoponePhase.GAME_END) {
            val myTeam = ScoponeGame.teamOf(localPlayer)
            val localWon = state.teamGameScore[myTeam] > state.teamGameScore[1 - myTeam]
            val localPoints = state.teamGameScore[myTeam]
            val localScope = state.scope[localPlayer]
            viewModelScope.launch {
                statsRepository.recordResult(
                    playerName = GameSession.localPlayerName ?: "Me",
                    gameType = "scopone",
                    won = localWon,
                    points = localPoints,
                    scope = localScope,
                    isSolo = isSolo,
                    opponentName = GameSession.opponentName ?: "",
                    maxConsecutiveScope = 0
                )
            }
        }
        if (state.phase == ScoponePhase.SESSION_END) {
            val myTeam = ScoponeGame.teamOf(localPlayer)
            val localWon = state.teamSessionWins[myTeam] >= state.teamSessionWins[1 - myTeam]
            if (localWon) SoundManager.playWin() else SoundManager.playLose()
            val winner = if (localWon) "1" else "2"
            viewModelScope.launch { _navigationEvent.emit("result/$winner/scopone") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!isSolo) btManager.disconnect()
        GameSession.isServer = false
        GameSession.scoponeLocalPlayer = 0
        GameSession.scoponeBtMode = 0
        GameSession.opponentName = null
        GameSession.partnerName = null
        GameSession.lastDevice = null
    }
}
