package com.fraccetta.scopabriscola.game.briscola

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Rank
import com.fraccetta.scopabriscola.domain.Suit

object BriscolaRules {

    fun cardPoints(rank: Rank): Int = when (rank) {
        Rank.ASSO -> 11
        Rank.TRE -> 10
        Rank.RE -> 4
        Rank.CAVALLO -> 3
        Rank.FANTE -> 2
        else -> 0
    }

    /**
     * Returns 1 or 2 (the winning player).
     * Rules:
     * - Briscola beats non-briscola.
     * - If both briscola or both same suit → higher numericValue wins.
     * - Different suits, neither briscola → first player wins.
     */
    fun trickWinner(firstCard: Card, secondCard: Card, firstPlayer: Int, briscolaSuit: Suit): Int {
        val secondPlayer = if (firstPlayer == 1) 2 else 1
        val firstIsBriscola = firstCard.suit == briscolaSuit
        val secondIsBriscola = secondCard.suit == briscolaSuit

        return when {
            firstIsBriscola && !secondIsBriscola -> firstPlayer
            secondIsBriscola && !firstIsBriscola -> secondPlayer
            firstCard.suit == secondCard.suit -> {
                if (secondCard.rank.numericValue > firstCard.rank.numericValue) secondPlayer else firstPlayer
            }
            else -> firstPlayer // different suits, no briscola → first player wins
        }
    }
}
