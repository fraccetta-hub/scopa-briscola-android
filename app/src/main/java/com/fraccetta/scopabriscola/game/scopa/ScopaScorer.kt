package com.fraccetta.scopabriscola.game.scopa

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Rank
import com.fraccetta.scopabriscola.domain.Suit

data class RoundScore(
    val carte: Int,       // 0 or 1
    val denari: Int,      // 0 or 1
    val settebello: Int,  // 0 or 1
    val primiera: Int,    // 0 or 1
    val scope: Int        // number of scope made this round
) {
    val total: Int get() = carte + denari + settebello + primiera + scope
}

object ScopaScorer {
    /** Primiera card value: 7=21, 6=18, A=16, 5=15, 4=14, 3=13, 2=12, F/C/R=10 */
    fun primieraValue(rank: Rank): Int = when (rank) {
        Rank.SETTE -> 21
        Rank.SEI -> 18
        Rank.ASSO -> 16
        Rank.CINQUE -> 15
        Rank.QUATTRO -> 14
        Rank.TRE -> 13
        Rank.DUE -> 12
        else -> 10  // FANTE, CAVALLO, RE
    }

    /**
     * Score a complete round. Parity (tie) awards the point to neither player.
     * Primiera: for each suit, take the highest primieraValue card held by that player (0 if none).
     * Sum the 4 suit values; higher total wins primiera (tie = nobody).
     */
    fun scoreRound(
        p1Captured: List<Card>, p1Scope: Int,
        p2Captured: List<Card>, p2Scope: Int
    ): Pair<RoundScore, RoundScore> {
        // Carte
        val p1Carte = if (p1Captured.size > p2Captured.size) 1 else 0
        val p2Carte = if (p2Captured.size > p1Captured.size) 1 else 0

        // Denari
        val p1Denari = p1Captured.count { it.suit == Suit.DENARI }
        val p2Denari = p2Captured.count { it.suit == Suit.DENARI }
        val p1DenariPoint = if (p1Denari > p2Denari) 1 else 0
        val p2DenariPoint = if (p2Denari > p1Denari) 1 else 0

        // Settebello
        val settebello = Card(Rank.SETTE, Suit.DENARI)
        val p1Settebello = if (p1Captured.contains(settebello)) 1 else 0
        val p2Settebello = if (p2Captured.contains(settebello)) 1 else 0

        // Primiera
        val p1PrimieraTotal = Suit.entries.sumOf { suit ->
            p1Captured.filter { it.suit == suit }.maxOfOrNull { primieraValue(it.rank) } ?: 0
        }
        val p2PrimieraTotal = Suit.entries.sumOf { suit ->
            p2Captured.filter { it.suit == suit }.maxOfOrNull { primieraValue(it.rank) } ?: 0
        }
        val p1Primiera = if (p1PrimieraTotal > p2PrimieraTotal) 1 else 0
        val p2Primiera = if (p2PrimieraTotal > p1PrimieraTotal) 1 else 0

        return Pair(
            RoundScore(p1Carte, p1DenariPoint, p1Settebello, p1Primiera, p1Scope),
            RoundScore(p2Carte, p2DenariPoint, p2Settebello, p2Primiera, p2Scope)
        )
    }
}
