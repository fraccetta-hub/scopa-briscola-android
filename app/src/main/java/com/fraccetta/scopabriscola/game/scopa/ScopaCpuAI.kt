package com.fraccetta.scopabriscola.game.scopa

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Rank
import com.fraccetta.scopabriscola.domain.Suit

object ScopaCpuAI {

    private val settebello = Card(Rank.SETTE, Suit.DENARI)

    private fun primieraValue(card: Card): Int = ScopaScorer.primieraValue(card.rank)

    private fun captureScore(group: List<Card>): Int = group.sumOf { primieraValue(it) }

    fun choosePlay(state: ScopaState): Card {
        val hand = state.player2Hand
        val table = state.tableCards

        // Try each card in hand to find best play
        data class Option(val card: Card, val capture: List<Card>?, val isScopa: Boolean, val score: Int)

        val options = hand.map { card ->
            val captures = ScopaRules.validCaptures(card, table)
            if (captures.isEmpty()) {
                Option(card, null, false, 0)
            } else {
                val best = captures.maxByOrNull { captureScore(it) }!!
                val afterCapture = table - best.toSet()
                val isLastPlay = hand.size == 1 && state.deckCards.isEmpty() && state.player1Hand.isEmpty()
                val scopa = ScopaRules.isScopa(afterCapture, isLastPlay)
                Option(card, best, scopa, captureScore(best))
            }
        }

        val withCapture = options.filter { it.capture != null }
        if (withCapture.isNotEmpty()) {
            // Priority 1: scopa
            withCapture.firstOrNull { it.isScopa }?.let { return it.card }
            // Priority 2: capture settebello
            withCapture.firstOrNull { it.capture!!.contains(settebello) }?.let { return it.card }
            // Priority 3: best primiera score
            return withCapture.maxByOrNull { it.score }!!.card
        }

        // No capture: discard lowest-value card, avoid discarding 7s and settebello
        return hand.minByOrNull { card ->
            val base = primieraValue(card)
            // Extra weight to keep 7s and denari cards (useful for future captures)
            val keepBonus = if (card.rank == Rank.SETTE) 50 else 0
            val denariBonus = if (card.suit == Suit.DENARI) 20 else 0
            base + keepBonus + denariBonus
        }!!
    }

    fun chooseCapture(state: ScopaState): List<Card> {
        val captures = state.pendingCaptures
        if (captures.isEmpty()) return emptyList()
        // Pick the capture group with the best primiera score
        return captures.maxByOrNull { group -> group.sumOf { primieraValue(it) } }!!
    }
}
