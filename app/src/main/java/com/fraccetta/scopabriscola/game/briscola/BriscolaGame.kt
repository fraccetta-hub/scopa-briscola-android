package com.fraccetta.scopabriscola.game.briscola

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck
import com.fraccetta.scopabriscola.domain.Suit
import kotlinx.serialization.Serializable

@Serializable
data class BriscolaState(
    val briscolaSuit: Suit,
    val briscolaCard: Card,
    val deckCards: List<Card>,           // excludes briscolaCard
    val player1Hand: List<Card>,
    val player2Hand: List<Card>,
    val player1Captured: List<Card>,
    val player2Captured: List<Card>,
    val player1SessionPoints: Int,
    val player2SessionPoints: Int,
    val currentPlayer: Int,
    val firstCardPlayed: Card?,
    val firstPlayerOfTrick: Int,
    val phase: BriscolaPhase,
    val briscolaCardTaken: Boolean = false
)

@Serializable
enum class BriscolaPhase { WAITING_FIRST, WAITING_SECOND, GAME_END, SESSION_END }

class BriscolaGame(val winPoints: Int = 121) {

    fun newSession(): BriscolaState {
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
            player1SessionPoints = 0,
            player2SessionPoints = 0,
            currentPlayer = 1,
            firstCardPlayed = null,
            firstPlayerOfTrick = 1,
            phase = BriscolaPhase.WAITING_FIRST,
            briscolaCardTaken = false
        )
    }

    fun newGame(existingState: BriscolaState): BriscolaState {
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
            player1SessionPoints = existingState.player1SessionPoints,
            player2SessionPoints = existingState.player2SessionPoints,
            currentPlayer = 1,
            firstCardPlayed = null,
            firstPlayerOfTrick = 1,
            phase = BriscolaPhase.WAITING_FIRST,
            briscolaCardTaken = false
        )
    }

    fun apply(state: BriscolaState, card: Card): BriscolaState {
        return when (state.phase) {
            BriscolaPhase.WAITING_FIRST -> handleFirstCard(state, card)
            BriscolaPhase.WAITING_SECOND -> handleSecondCard(state, card)
            else -> state // no-op in GAME_END / SESSION_END
        }
    }

    private fun handleFirstCard(state: BriscolaState, card: Card): BriscolaState {
        val newP1Hand = if (state.currentPlayer == 1) state.player1Hand - card else state.player1Hand
        val newP2Hand = if (state.currentPlayer == 2) state.player2Hand - card else state.player2Hand
        val nextPlayer = if (state.currentPlayer == 1) 2 else 1
        return state.copy(
            player1Hand = newP1Hand,
            player2Hand = newP2Hand,
            firstCardPlayed = card,
            firstPlayerOfTrick = state.currentPlayer,
            currentPlayer = nextPlayer,
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

        // Drawing after trick
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
                    winnerHand.add(state.briscolaCard)
                    briscolaCardTaken = true
                }
            }
            deck.isEmpty() && !briscolaCardTaken -> {
                winnerHand.add(state.briscolaCard)
                briscolaCardTaken = true
            }
        }

        val (p1Hand, p2Hand) = if (winner == 1) {
            Pair(winnerHand, loserHand)
        } else {
            Pair(loserHand, winnerHand)
        }

        val afterTrickState = state.copy(
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

        // Check end of game: both hands empty
        return if (afterTrickState.player1Hand.isEmpty() && afterTrickState.player2Hand.isEmpty()) {
            endGame(afterTrickState)
        } else {
            afterTrickState
        }
    }

    private fun endGame(state: BriscolaState): BriscolaState {
        val p1Points = state.player1Captured.sumOf { BriscolaRules.cardPoints(it.rank) }
        val p2Points = state.player2Captured.sumOf { BriscolaRules.cardPoints(it.rank) }

        val newP1Session = state.player1SessionPoints + p1Points
        val newP2Session = state.player2SessionPoints + p2Points

        val phase = if (newP1Session >= winPoints || newP2Session >= winPoints) {
            BriscolaPhase.SESSION_END
        } else {
            BriscolaPhase.GAME_END
        }

        return state.copy(
            player1SessionPoints = newP1Session,
            player2SessionPoints = newP2Session,
            phase = phase
        )
    }
}
