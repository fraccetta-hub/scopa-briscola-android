package com.fraccetta.scopabriscola.game.scopa

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ScopaState(
    // Current match cards
    val tableCards: List<Card>,
    val player1Hand: List<Card>,
    val player2Hand: List<Card>,
    val player1Captured: List<Card>,
    val player2Captured: List<Card>,
    val player1Scope: Int,           // scope in current match
    val player2Scope: Int,
    val deckCards: List<Card>,
    val currentPlayer: Int,
    val phase: ScopaPhase,
    val pendingCaptures: List<List<Card>>,
    val lastCapturerId: Int = 1,
    val matchStarter: Int = 1,
    @Transient val lastCapture: Pair<Int, List<Card>>? = null,

    // Game score (accumulated across matches within current game)
    val player1GameScore: Int = 0,
    val player2GameScore: Int = 0,
    val targetScore: Int = 11,

    // Session wins (best of 3 games)
    val player1SessionWins: Int = 0,
    val player2SessionWins: Int = 0
)

@Serializable
enum class ScopaPhase {
    WAITING_PLAY,
    WAITING_CAPTURE_CHOICE,
    MATCH_END,     // end of single match — show match result
    GAME_END,      // someone reached targetScore — show game result
    SESSION_END    // someone won 2 games — navigate to ResultScreen
}

sealed class ScopaAction {
    data class PlayCard(val card: Card) : ScopaAction()
    data class ChooseCapture(val captureGroup: List<Card>) : ScopaAction()
    object NextMatch : ScopaAction()   // from MATCH_END → new match in same game
    object NextGame : ScopaAction()    // from GAME_END → new game (checks session end)
}

class ScopaGame {

    fun newSession(targetScore: Int = 11): ScopaState {
        val (tableCards, p1Hand, p2Hand, deck) = dealInitial()
        return ScopaState(
            tableCards = tableCards,
            player1Hand = p1Hand,
            player2Hand = p2Hand,
            player1Captured = emptyList(),
            player2Captured = emptyList(),
            player1Scope = 0,
            player2Scope = 0,
            currentPlayer = 1,
            phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(),
            lastCapture = null,
            deckCards = deck,
            player1GameScore = 0,
            player2GameScore = 0,
            targetScore = targetScore,
            player1SessionWins = 0,
            player2SessionWins = 0
        )
    }

    fun apply(state: ScopaState, action: ScopaAction): ScopaState {
        return when (action) {
            is ScopaAction.PlayCard -> handlePlayCard(state, action.card)
            is ScopaAction.ChooseCapture -> handleChooseCapture(state, action.captureGroup)
            is ScopaAction.NextMatch -> handleNextMatch(state)
            is ScopaAction.NextGame -> handleNextGame(state)
        }
    }

    private fun dealInitial(): Quad {
        val deck = Deck.shuffled().toMutableList()
        val tableCards = deal(deck, 4)
        val p1Hand = deal(deck, 3)
        val p2Hand = deal(deck, 3)
        return Quad(tableCards, p1Hand, p2Hand, deck.toList())
    }

    private data class Quad(
        val tableCards: List<Card>,
        val p1Hand: List<Card>,
        val p2Hand: List<Card>,
        val deck: List<Card>
    )

    private fun deal(deck: MutableList<Card>, n: Int): List<Card> {
        val hand = deck.take(n)
        repeat(n) { if (deck.isNotEmpty()) deck.removeAt(0) }
        return hand
    }

    private fun handlePlayCard(state: ScopaState, card: Card): ScopaState {
        val newP1Hand = if (state.currentPlayer == 1) state.player1Hand - card else state.player1Hand
        val newP2Hand = if (state.currentPlayer == 2) state.player2Hand - card else state.player2Hand
        val captures = ScopaRules.validCaptures(card, state.tableCards)

        return when {
            captures.isEmpty() -> {
                val afterPlay = state.copy(
                    tableCards = state.tableCards + card,
                    player1Hand = newP1Hand,
                    player2Hand = newP2Hand,
                    currentPlayer = if (state.currentPlayer == 1) 2 else 1,
                    lastCapture = null,
                    phase = ScopaPhase.WAITING_PLAY
                ).checkDealIfNeeded()
                if (afterPlay.player1Hand.isEmpty() && afterPlay.player2Hand.isEmpty() && afterPlay.deckCards.isEmpty()) {
                    endMatch(afterPlay, afterPlay.lastCapturerId)
                } else afterPlay
            }
            captures.size == 1 -> {
                executeCapture(state.copy(player1Hand = newP1Hand, player2Hand = newP2Hand), card, captures[0])
            }
            else -> {
                state.copy(
                    player1Hand = newP1Hand,
                    player2Hand = newP2Hand,
                    phase = ScopaPhase.WAITING_CAPTURE_CHOICE,
                    pendingCaptures = captures
                )
            }
        }
    }

    private fun handleChooseCapture(state: ScopaState, captureGroup: List<Card>): ScopaState {
        val allCards = Deck.full().toSet()
        val accountedFor = (state.player1Hand + state.player2Hand + state.tableCards +
            state.player1Captured + state.player2Captured + state.deckCards).toSet()
        val playedCard = allCards.first { it !in accountedFor }
        val baseState = state.copy(phase = ScopaPhase.WAITING_PLAY, pendingCaptures = emptyList())
        return executeCapture(baseState, playedCard, captureGroup)
    }

    private fun handleNextMatch(state: ScopaState): ScopaState {
        val (tableCards, p1Hand, p2Hand, deck) = dealInitial()
        val nextStarter = if (state.matchStarter == 1) 2 else 1
        return state.copy(
            tableCards = tableCards,
            player1Hand = p1Hand,
            player2Hand = p2Hand,
            player1Captured = emptyList(),
            player2Captured = emptyList(),
            player1Scope = 0,
            player2Scope = 0,
            currentPlayer = nextStarter,
            matchStarter = nextStarter,
            phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(),
            lastCapture = null,
            deckCards = deck
        )
    }

    private fun handleNextGame(state: ScopaState): ScopaState {
        val gameWinner = when {
            state.player1GameScore > state.player2GameScore -> 1
            state.player2GameScore > state.player1GameScore -> 2
            else -> 1 // tie goes to player 1
        }
        val newP1SessionWins = if (gameWinner == 1) state.player1SessionWins + 1 else state.player1SessionWins
        val newP2SessionWins = if (gameWinner == 2) state.player2SessionWins + 1 else state.player2SessionWins

        if (newP1SessionWins >= 2 || newP2SessionWins >= 2) {
            return state.copy(
                player1SessionWins = newP1SessionWins,
                player2SessionWins = newP2SessionWins,
                phase = ScopaPhase.SESSION_END
            )
        }

        // Reset game, start new match — alternate who starts the new game
        val nextStarter = if (state.matchStarter == 1) 2 else 1
        val (tableCards, p1Hand, p2Hand, deck) = dealInitial()
        return state.copy(
            tableCards = tableCards,
            player1Hand = p1Hand,
            player2Hand = p2Hand,
            player1Captured = emptyList(),
            player2Captured = emptyList(),
            player1Scope = 0,
            player2Scope = 0,
            player1GameScore = 0,
            player2GameScore = 0,
            player1SessionWins = newP1SessionWins,
            player2SessionWins = newP2SessionWins,
            currentPlayer = nextStarter,
            matchStarter = nextStarter,
            phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(),
            lastCapture = null,
            deckCards = deck
        )
    }

    private fun executeCapture(state: ScopaState, playedCard: Card, captureGroup: List<Card>): ScopaState {
        val newTableCards = state.tableCards - captureGroup.toSet()
        val capturedCards = captureGroup + playedCard
        val isLastCardOfRound = state.deckCards.isEmpty() && state.player1Hand.isEmpty() && state.player2Hand.isEmpty()
        val isScopa = ScopaRules.isScopa(newTableCards, isLastCardOfRound)

        val newP1Captured = if (state.currentPlayer == 1) state.player1Captured + capturedCards else state.player1Captured
        val newP2Captured = if (state.currentPlayer == 2) state.player2Captured + capturedCards else state.player2Captured
        val newP1Scope = if (state.currentPlayer == 1 && isScopa) state.player1Scope + 1 else state.player1Scope
        val newP2Scope = if (state.currentPlayer == 2 && isScopa) state.player2Scope + 1 else state.player2Scope

        val afterCapture = state.copy(
            tableCards = newTableCards,
            player1Captured = newP1Captured,
            player2Captured = newP2Captured,
            player1Scope = newP1Scope,
            player2Scope = newP2Scope,
            currentPlayer = if (state.currentPlayer == 1) 2 else 1,
            lastCapturerId = state.currentPlayer,
            lastCapture = Pair(state.currentPlayer, capturedCards),
            phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList()
        )

        return if (afterCapture.player1Hand.isEmpty() && afterCapture.player2Hand.isEmpty()) {
            if (afterCapture.deckCards.isNotEmpty()) afterCapture.checkDealIfNeeded()
            else endMatch(afterCapture, state.currentPlayer)
        } else afterCapture
    }

    private fun ScopaState.checkDealIfNeeded(): ScopaState {
        if (player1Hand.isEmpty() && player2Hand.isEmpty() && deckCards.isNotEmpty()) {
            val deck = deckCards.toMutableList()
            val p1Hand = deck.take(3)
            repeat(minOf(3, deck.size)) { deck.removeAt(0) }
            val p2Hand = deck.take(3)
            repeat(minOf(3, deck.size)) { deck.removeAt(0) }
            return copy(player1Hand = p1Hand, player2Hand = p2Hand, deckCards = deck.toList())
        }
        return this
    }

    private fun endMatch(state: ScopaState, lastCapturer: Int): ScopaState {
        val remainingTable = state.tableCards
        val finalP1Captured = if (lastCapturer == 1) state.player1Captured + remainingTable else state.player1Captured
        val finalP2Captured = if (lastCapturer == 2) state.player2Captured + remainingTable else state.player2Captured

        val (p1Score, p2Score) = ScopaScorer.scoreRound(
            finalP1Captured, state.player1Scope,
            finalP2Captured, state.player2Scope
        )

        val newP1GameScore = state.player1GameScore + p1Score.total
        val newP2GameScore = state.player2GameScore + p2Score.total

        val afterMatchState = state.copy(
            tableCards = emptyList(),
            player1Captured = finalP1Captured,
            player2Captured = finalP2Captured,
            player1GameScore = newP1GameScore,
            player2GameScore = newP2GameScore
        )

        val p1Wins = newP1GameScore >= state.targetScore
        val p2Wins = newP2GameScore >= state.targetScore

        return when {
            p1Wins && p2Wins -> when {
                newP1GameScore > newP2GameScore -> afterMatchState.copy(phase = ScopaPhase.GAME_END)
                newP2GameScore > newP1GameScore -> afterMatchState.copy(phase = ScopaPhase.GAME_END)
                else -> afterMatchState.copy(phase = ScopaPhase.MATCH_END) // exact tie, one more match
            }
            p1Wins || p2Wins -> afterMatchState.copy(phase = ScopaPhase.GAME_END)
            else -> afterMatchState.copy(phase = ScopaPhase.MATCH_END)
        }
    }
}
