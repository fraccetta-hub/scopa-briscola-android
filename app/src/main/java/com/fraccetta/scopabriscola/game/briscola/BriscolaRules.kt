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

    // Briscola trick strength: Asso > Tre > Re > Cavallo > Fante > 7 > 6 > 5 > 4 > 2
    private fun rankStrength(rank: Rank): Int = when (rank) {
        Rank.ASSO    -> 10
        Rank.TRE     -> 9
        Rank.RE      -> 8
        Rank.CAVALLO -> 7
        Rank.FANTE   -> 6
        Rank.SETTE   -> 5
        Rank.SEI     -> 4
        Rank.CINQUE  -> 3
        Rank.QUATTRO -> 2
        Rank.DUE     -> 1
    }

    /**
     * Returns 1 or 2 (the winning player).
     * - Briscola beats non-briscola.
     * - Both briscola or both same suit → higher rankStrength wins.
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
                if (rankStrength(secondCard.rank) > rankStrength(firstCard.rank)) secondPlayer else firstPlayer
            }
            else -> firstPlayer // different suits, no briscola → first player wins
        }
    }
}
