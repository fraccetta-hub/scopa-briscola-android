package com.fraccetta.scopabriscola.game.scopone

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck
import com.fraccetta.scopabriscola.game.scopa.ScopaRules
import com.fraccetta.scopabriscola.game.scopa.ScopaScorer
import kotlinx.serialization.Serializable

@Serializable
data class ScoponeCapture(val playerWho: Int, val cards: List<Card>)

@Serializable
data class ScoponeState(
    val tableCards: List<Card>,
    val hands: List<List<Card>>,        // hands[0..3]
    val captured: List<List<Card>>,     // captured[0..3]
    val scope: List<Int>,               // scope[0..3]
    val currentPlayer: Int,
    val phase: ScoponePhase,
    val pendingCaptures: List<List<Card>>,
    val lastCapturerId: Int = 0,
    val matchStarter: Int = 0,
    val lastCapture: ScoponeCapture? = null,
    val teamGameScore: List<Int> = listOf(0, 0),
    val teamSessionWins: List<Int> = listOf(0, 0),
    val targetScore: Int = 11
)

@Serializable
enum class ScoponePhase {
    WAITING_PLAY,
    WAITING_CAPTURE_CHOICE,
    MATCH_END,
    GAME_END,
    SESSION_END
}

sealed class ScoponeAction {
    data class PlayCard(val card: Card) : ScoponeAction()
    data class ChooseCapture(val captureGroup: List<Card>) : ScoponeAction()
    object NextMatch : ScoponeAction()
    object NextGame : ScoponeAction()
}

class ScoponeGame {
    companion object {
        // Teams: 0+2 = team 0, 1+3 = team 1
        fun teamOf(player: Int): Int = if (player % 2 == 0) 0 else 1
        // Counter-clockwise: 0→3→2→1→0
        fun nextPlayer(p: Int): Int = when (p) { 0 -> 3; 1 -> 0; 2 -> 1; else -> 2 }
    }

    fun newSession(targetScore: Int = 11): ScoponeState {
        val deck = Deck.shuffled().toMutableList()
        val hands = listOf(deal(deck, 10), deal(deck, 10), deal(deck, 10), deal(deck, 10))
        return ScoponeState(
            tableCards = emptyList(),
            hands = hands,
            captured = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            scope = listOf(0, 0, 0, 0),
            currentPlayer = 0,
            phase = ScoponePhase.WAITING_PLAY,
            pendingCaptures = emptyList(),
            matchStarter = 0
        )
    }

    fun apply(state: ScoponeState, action: ScoponeAction): ScoponeState = when (action) {
        is ScoponeAction.PlayCard -> handlePlayCard(state, action.card)
        is ScoponeAction.ChooseCapture -> handleChooseCapture(state, action.captureGroup)
        is ScoponeAction.NextMatch -> handleNextMatch(state)
        is ScoponeAction.NextGame -> handleNextGame(state)
    }

    private fun deal(deck: MutableList<Card>, n: Int): List<Card> {
        val hand = deck.take(n).toList()
        repeat(n) { if (deck.isNotEmpty()) deck.removeAt(0) }
        return hand
    }

    private fun handlePlayCard(state: ScoponeState, card: Card): ScoponeState {
        val cp = state.currentPlayer
        val newHands = state.hands.toMutableList()
        newHands[cp] = state.hands[cp] - card

        val captures = ScopaRules.validCaptures(card, state.tableCards)
        val baseState = state.copy(hands = newHands)

        return when {
            captures.isEmpty() -> {
                val afterPlay = baseState.copy(
                    tableCards = state.tableCards + card,
                    currentPlayer = nextPlayer(cp),
                    lastCapture = null,
                    phase = ScoponePhase.WAITING_PLAY,
                    pendingCaptures = emptyList()
                )
                if (afterPlay.hands.all { it.isEmpty() }) endMatch(afterPlay, afterPlay.lastCapturerId)
                else afterPlay
            }
            captures.size == 1 -> executeCapture(baseState, card, captures[0])
            else -> baseState.copy(
                phase = ScoponePhase.WAITING_CAPTURE_CHOICE,
                pendingCaptures = captures
            )
        }
    }

    private fun handleChooseCapture(state: ScoponeState, captureGroup: List<Card>): ScoponeState {
        val allCards = Deck.full().toSet()
        val accountedFor = (state.hands.flatten() + state.tableCards + state.captured.flatten()).toSet()
        val playedCard = allCards.first { it !in accountedFor }
        val baseState = state.copy(phase = ScoponePhase.WAITING_PLAY, pendingCaptures = emptyList())
        return executeCapture(baseState, playedCard, captureGroup)
    }

    private fun executeCapture(state: ScoponeState, playedCard: Card, captureGroup: List<Card>): ScoponeState {
        val cp = state.currentPlayer
        val newTableCards = state.tableCards - captureGroup.toSet()
        val capturedCards = captureGroup + playedCard

        // In Scopone scientifico: last trick IS a scopa
        val isScopa = newTableCards.isEmpty()

        val newCaptured = state.captured.toMutableList()
        newCaptured[cp] = state.captured[cp] + capturedCards

        val newScope = state.scope.toMutableList()
        if (isScopa) newScope[cp] = state.scope[cp] + 1

        val afterCapture = state.copy(
            tableCards = newTableCards,
            captured = newCaptured,
            scope = newScope,
            currentPlayer = nextPlayer(cp),
            lastCapturerId = cp,
            lastCapture = ScoponeCapture(playerWho = cp, cards = capturedCards),
            phase = ScoponePhase.WAITING_PLAY,
            pendingCaptures = emptyList()
        )

        return if (afterCapture.hands.all { it.isEmpty() }) endMatch(afterCapture, cp)
        else afterCapture
    }

    private fun handleNextMatch(state: ScoponeState): ScoponeState {
        val deck = Deck.shuffled().toMutableList()
        val hands = listOf(deal(deck, 10), deal(deck, 10), deal(deck, 10), deal(deck, 10))
        val nextStarter = nextPlayer(state.matchStarter)
        return state.copy(
            tableCards = emptyList(),
            hands = hands,
            captured = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            scope = listOf(0, 0, 0, 0),
            currentPlayer = nextStarter,
            matchStarter = nextStarter,
            phase = ScoponePhase.WAITING_PLAY,
            pendingCaptures = emptyList(),
            lastCapture = null
        )
    }

    private fun handleNextGame(state: ScoponeState): ScoponeState {
        val gameWinner = if (state.teamGameScore[0] >= state.teamGameScore[1]) 0 else 1
        val newWins = state.teamSessionWins.toMutableList()
        newWins[gameWinner]++

        if (newWins.any { it >= 2 }) {
            return state.copy(teamSessionWins = newWins, phase = ScoponePhase.SESSION_END)
        }

        val nextStarter = nextPlayer(state.matchStarter)
        val deck = Deck.shuffled().toMutableList()
        val hands = listOf(deal(deck, 10), deal(deck, 10), deal(deck, 10), deal(deck, 10))
        return state.copy(
            tableCards = emptyList(),
            hands = hands,
            captured = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            scope = listOf(0, 0, 0, 0),
            teamGameScore = listOf(0, 0),
            teamSessionWins = newWins,
            currentPlayer = nextStarter,
            matchStarter = nextStarter,
            phase = ScoponePhase.WAITING_PLAY,
            pendingCaptures = emptyList(),
            lastCapture = null
        )
    }

    private fun endMatch(state: ScoponeState, lastCapturer: Int): ScoponeState {
        val newCaptured = state.captured.toMutableList()
        newCaptured[lastCapturer] = state.captured[lastCapturer] + state.tableCards

        val team0Captured = newCaptured[0] + newCaptured[2]
        val team0Scope = state.scope[0] + state.scope[2]
        val team1Captured = newCaptured[1] + newCaptured[3]
        val team1Scope = state.scope[1] + state.scope[3]

        val (score0, score1) = ScopaScorer.scoreRound(team0Captured, team0Scope, team1Captured, team1Scope)

        val newTeamScore = listOf(
            state.teamGameScore[0] + score0.total,
            state.teamGameScore[1] + score1.total
        )

        val afterMatchState = state.copy(
            tableCards = emptyList(),
            captured = newCaptured,
            teamGameScore = newTeamScore
        )

        val t0Wins = newTeamScore[0] >= state.targetScore
        val t1Wins = newTeamScore[1] >= state.targetScore

        return when {
            t0Wins && t1Wins -> when {
                newTeamScore[0] != newTeamScore[1] -> afterMatchState.copy(phase = ScoponePhase.GAME_END)
                else -> afterMatchState.copy(phase = ScoponePhase.MATCH_END)
            }
            t0Wins || t1Wins -> afterMatchState.copy(phase = ScoponePhase.GAME_END)
            else -> afterMatchState.copy(phase = ScoponePhase.MATCH_END)
        }
    }
}
