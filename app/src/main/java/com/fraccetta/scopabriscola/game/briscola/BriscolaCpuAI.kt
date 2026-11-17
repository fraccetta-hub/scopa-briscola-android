package com.fraccetta.scopabriscola.game.briscola

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Suit

object BriscolaCpuAI {

    private fun cardValue(card: Card): Int = BriscolaRules.cardPoints(card.rank)

    fun choosePlay(state: BriscolaState): Card {
        val hand = state.player2Hand
        val briscolaSuit = state.briscolaCard.suit

        return when (state.phase) {
            BriscolaPhase.WAITING_FIRST -> {
                // Going first: play a low-value non-briscola card to preserve strong cards
                val nonBriscola = hand.filter { it.suit != briscolaSuit }
                val pool = nonBriscola.ifEmpty { hand }
                // Among zero-point cards, pick any; else pick lowest points
                pool.filter { cardValue(it) == 0 }.randomOrNull()
                    ?: pool.minByOrNull { cardValue(it) }!!
            }
            BriscolaPhase.WAITING_SECOND -> {
                val firstCard = state.firstCardPlayed ?: return hand.minByOrNull { cardValue(it) }!!
                val trickPoints = cardValue(firstCard)

                // Try to win if worth it
                val winningCards = hand.filter { candidate ->
                    BriscolaRules.trickWinner(firstCard, candidate, state.firstPlayerOfTrick, briscolaSuit) == 2
                }

                if (trickPoints > 0 && winningCards.isNotEmpty()) {
                    // Win with the cheapest winning card
                    winningCards.minByOrNull { cardValue(it) }!!
                } else {
                    // Trick not worth winning, or can't win: dump lowest non-briscola
                    val nonBriscola = hand.filter { it.suit != briscolaSuit }
                    val pool = nonBriscola.ifEmpty { hand }
                    pool.filter { cardValue(it) == 0 }.minByOrNull { it.rank.numericValue }
                        ?: pool.minByOrNull { cardValue(it) }!!
                }
            }
            else -> hand.randomOrNull() ?: hand.first()
        }
    }
}
