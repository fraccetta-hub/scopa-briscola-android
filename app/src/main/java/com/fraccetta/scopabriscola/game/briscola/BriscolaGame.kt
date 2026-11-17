package com.fraccetta.scopabriscola.game.briscola

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck
import com.fraccetta.scopabriscola.domain.Suit
import kotlinx.serialization.Serializable

@Serializable
data class BriscolaState(
    // Current match cards
    val briscolaSuit: Suit,
    val briscolaCard: Card,
    val deckCards: List<Card>,
    val player1Hand: List<Card>,
    val player2Hand: List<Card>,
    val player1Captured: List<Card>,
    val player2Captured: List<Card>,
    val currentPlayer: Int,
    val firstCardPlayed: Card?,
    val firstPlayerOfTrick: Int,
    val phase: BriscolaPhase,
    val briscolaCardTaken: Boolean = false,

    // Points from the last finished match (for MATCH_END display)
    val player1MatchPoints: Int = 0,
    val player2MatchPoints: Int = 0,

    // Game: 2 matches per game, cumulative points
    val player1GamePoints: Int = 0,
    val player2GamePoints: Int = 0,
    val matchesPlayedInGame: Int = 0,

    // Session wins (best of 3 games)
    val player1SessionWins: Int = 0,
    val player2SessionWins: Int = 0
)

@Serializable
enum class BriscolaPhase {
    WAITING_FIRST,
    WAITING_SECOND,
    MATCH_END,     // end of a single match — show match result
    GAME_END,      // 2 matches played — show game result
    SESSION_END    // someone won 2 games — navigate to ResultScreen
}

sealed class BriscolaAction {
    data class PlayCard(val card: Card) : BriscolaAction()
    object NextMatch : BriscolaAction()   // from MATCH_END → new match in same game
    object NextGame : BriscolaAction()    // from GAME_END → new game (checks session end)
}

class BriscolaGame {

    fun newSession(): BriscolaState {
        return dealNewMatch(
            player1GamePoints = 0,
            player2GamePoints = 0,
            matchesPlayedInGame = 0,
            player1SessionWins = 0,
            player2SessionWins = 0
        )
    }

    fun apply(state: BriscolaState, action: BriscolaAction): BriscolaState {
        return when (action) {
            is BriscolaAction.PlayCard -> applyCard(state, action.card)
            is BriscolaAction.NextMatch -> handleNextMatch(state)
            is BriscolaAction.NextGame -> handleNextGame(state)
        }
    }

    private fun applyCard(state: BriscolaState, card: Card): BriscolaState {
        return when (state.phase) {
            BriscolaPhase.WAITING_FIRST -> handleFirstCard(state, card)
            BriscolaPhase.WAITING_SECOND -> handleSecondCard(state, card)
            else -> state
        }
    }

    private fun handleNextMatch(state: BriscolaState): BriscolaState {
        return dealNewMatch(
            player1GamePoints = state.player1GamePoints,
            player2GamePoints = state.player2GamePoints,
            matchesPlayedInGame = state.matchesPlayedInGame,
            player1SessionWins = state.player1SessionWins,
            player2SessionWins = state.player2SessionWins
        )
    }

    private fun handleNextGame(state: BriscolaState): BriscolaState {
        val gameWinner = when {
            state.player1GamePoints > state.player2GamePoints -> 1
            state.player2GamePoints > state.player1GamePoints -> 2
            else -> 1 // tie to player 1
        }
        val newP1SessionWins = if (gameWinner == 1) state.player1SessionWins + 1 else state.player1SessionWins
        val newP2SessionWins = if (gameWinner == 2) state.player2SessionWins + 1 else state.player2SessionWins

        if (newP1SessionWins >= 2 || newP2SessionWins >= 2) {
            return state.copy(
                player1SessionWins = newP1SessionWins,
                player2SessionWins = newP2SessionWins,
                phase = BriscolaPhase.SESSION_END
            )
        }

        return dealNewMatch(
            player1GamePoints = 0,
            player2GamePoints = 0,
            matchesPlayedInGame = 0,
            player1SessionWins = newP1SessionWins,
            player2SessionWins = newP2SessionWins
        )
    }

    private fun dealNewMatch(
        player1GamePoints: Int,
        player2GamePoints: Int,
        matchesPlayedInGame: Int,
        player1SessionWins: Int,
        player2SessionWins: Int
    ): BriscolaState {
        val deck = Deck.shuffled().toMutableList()
        val p1Hand = deck.take(3).also { repeat(3) { deck.removeAt(0) } }
        val p2Hand = deck.take(3).also { repeat(3) { deck.removeAt(0) } }
        val briscolaCard = deck.last()
        deck.removeAt(deck.size - 1)
        return BriscolaState(
            briscolaSuit = briscolaCard.suit,
            briscolaCard = briscolaCard,
            deckCards = deck.toList(),
            player1Hand = p1Hand,
            player2Hand = p2Hand,
            player1Captured = emptyList(),
            player2Captured = emptyList(),
            currentPlayer = 1,
            firstCardPlayed = null,
            firstPlayerOfTrick = 1,
            phase = BriscolaPhase.WAITING_FIRST,
            briscolaCardTaken = false,
            player1MatchPoints = 0,
            player2MatchPoints = 0,
            player1GamePoints = player1GamePoints,
            player2GamePoints = player2GamePoints,
            matchesPlayedInGame = matchesPlayedInGame,
            player1SessionWins = player1SessionWins,
            player2SessionWins = player2SessionWins
        )
    }

    private fun handleFirstCard(state: BriscolaState, card: Card): BriscolaState {
        val newP1Hand = if (state.currentPlayer == 1) state.player1Hand - card else state.player1Hand
        val newP2Hand = if (state.currentPlayer == 2) state.player2Hand - card else state.player2Hand
        return state.copy(
            player1Hand = newP1Hand,
            player2Hand = newP2Hand,
            firstCardPlayed = card,
            firstPlayerOfTrick = state.currentPlayer,
            currentPlayer = if (state.currentPlayer == 1) 2 else 1,
            phase = BriscolaPhase.WAITING_SECOND
        )
    }

    private fun handleSecondCard(state: BriscolaState, card: Card): BriscolaState {
        val firstCard = state.firstCardPlayed!!
        val newP1Hand = if (state.currentPlayer == 1) state.player1Hand - card else state.player1Hand
        val newP2Hand = if (state.currentPlayer == 2) state.player2Hand - card else state.player2Hand

        val winner = BriscolaRules.trickWinner(firstCard, card, state.firstPlayerOfTrick, state.briscolaSuit)
        val loser = if (winner == 1) 2 else 1
        val trickCards = listOf(firstCard, card)
        val newP1Captured = if (winner == 1) state.player1Captured + trickCards else state.player1Captured
        val newP2Captured = if (winner == 2) state.player2Captured + trickCards else state.player2Captured

        var deck = state.deckCards.toMutableList()
        var briscolaCardTaken = state.briscolaCardTaken
        var winnerHand = if (winner == 1) newP1Hand.toMutableList() else newP2Hand.toMutableList()
        var loserHand = if (loser == 1) newP1Hand.toMutableList() else newP2Hand.toMutableList()

        when {
            deck.size >= 2 -> {
                winnerHand.add(deck.removeAt(0))
                loserHand.add(deck.removeAt(0))
            }
            deck.size == 1 -> {
                winnerHand.add(deck.removeAt(0))
                if (!briscolaCardTaken) {
                    loserHand.add(state.briscolaCard)
                    briscolaCardTaken = true
                }
            }
            deck.isEmpty() && !briscolaCardTaken -> {
                winnerHand.add(state.briscolaCard)
                briscolaCardTaken = true
            }
        }

        val (p1Hand, p2Hand) = if (winner == 1) Pair(winnerHand, loserHand) else Pair(loserHand, winnerHand)

        val afterTrick = state.copy(
            deckCards = deck.toList(),
            player1Hand = p1Hand,
            player2Hand = p2Hand,
            player1Captured = newP1Captured,
            player2Captured = newP2Captured,
            currentPlayer = winner,
            firstCardPlayed = null,
            firstPlayerOfTrick = winner,
            phase = BriscolaPhase.WAITING_FIRST,
            briscolaCardTaken = briscolaCardTaken
        )

        return if (afterTrick.player1Hand.isEmpty() && afterTrick.player2Hand.isEmpty()) {
            endMatch(afterTrick)
        } else afterTrick
    }

    private fun endMatch(state: BriscolaState): BriscolaState {
        val p1Points = state.player1Captured.sumOf { BriscolaRules.cardPoints(it.rank) }
        val p2Points = state.player2Captured.sumOf { BriscolaRules.cardPoints(it.rank) }

        val newP1GamePoints = state.player1GamePoints + p1Points
        val newP2GamePoints = state.player2GamePoints + p2Points
        val newMatchesPlayed = state.matchesPlayedInGame + 1

        val phase = if (newMatchesPlayed >= 2) BriscolaPhase.GAME_END else BriscolaPhase.MATCH_END

        return state.copy(
            player1MatchPoints = p1Points,
            player2MatchPoints = p2Points,
            player1GamePoints = newP1GamePoints,
            player2GamePoints = newP2GamePoints,
            matchesPlayedInGame = newMatchesPlayed,
            phase = phase
        )
    }
}
