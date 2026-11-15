package com.fraccetta.scopabriscola.game.scopa

import com.fraccetta.scopabriscola.domain.Card

object ScopaRules {
    /**
     * Returns all valid capture groups for [playedCard] against [tableCards].
     * Rule: if any single card on the table matches [playedCard]'s rank,
     * return ONLY those single-card groups (mandatory single capture).
     * Otherwise return all subsets of [tableCards] whose numericValue sum
     * equals [playedCard]'s numericValue.
     * Returns empty list if no capture is possible.
     */
    fun validCaptures(playedCard: Card, tableCards: List<Card>): List<List<Card>> {
        val singles = tableCards.filter { it.rank == playedCard.rank }
        if (singles.isNotEmpty()) {
            return singles.map { listOf(it) }
        }
        val target = playedCard.rank.numericValue
        val results = mutableListOf<List<Card>>()
        val n = tableCards.size
        for (mask in 1 until (1 shl n)) {
            val subset = mutableListOf<Card>()
            var sum = 0
            for (i in 0 until n) {
                if (mask and (1 shl i) != 0) {
                    subset.add(tableCards[i])
                    sum += tableCards[i].rank.numericValue
                }
            }
            if (sum == target) {
                results.add(subset)
            }
        }
        return results
    }

    /**
     * @param tableCardsRemaining the table AFTER the capture group was removed
     * @param isLastCardOfRound true when this is the last play of the round
     * @return true if table is now empty AND not the last card of the round
     */
    fun isScopa(tableCardsRemaining: List<Card>, isLastCardOfRound: Boolean): Boolean =
        tableCardsRemaining.isEmpty() && !isLastCardOfRound
}
