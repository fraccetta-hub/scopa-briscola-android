package com.fraccetta.scopabriscola.game.scopone

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Rank
import com.fraccetta.scopabriscola.domain.Suit
import com.fraccetta.scopabriscola.game.scopa.ScopaRules
import com.fraccetta.scopabriscola.game.scopa.ScopaScorer

object ScoponeCpuAI {

    private val settebello = Card(Rank.SETTE, Suit.DENARI)

    private fun primieraValue(card: Card): Int = ScopaScorer.primieraValue(card.rank)
    private fun captureScore(group: List<Card>): Int = group.sumOf { primieraValue(it) }

    fun choosePlay(state: ScoponeState, playerIndex: Int): Card {
        val hand = state.hands[playerIndex]
        val table = state.tableCards
        val teamIndex = ScoponeGame.teamOf(playerIndex)
        val partnerIndex = if (playerIndex == 0) 2 else if (playerIndex == 2) 0 else if (playerIndex == 1) 3 else 1

        data class Option(val card: Card, val capture: List<Card>?, val isScopa: Boolean, val score: Int)

        val options = hand.map { card ->
            val captures = ScopaRules.validCaptures(card, table)
            if (captures.isEmpty()) {
                Option(card, null, false, 0)
            } else {
                val best = captures.maxByOrNull { captureScore(it) }!!
                val afterCapture = table - best.toSet()
                val scopa = afterCapture.isEmpty()
                Option(card, best, scopa, captureScore(best))
            }
        }

        val withCapture = options.filter { it.capture != null }
        if (withCapture.isNotEmpty()) {
            // Priority 1: scopa (always good in Scopone)
            withCapture.firstOrNull { it.isScopa }?.let { return it.card }
            // Priority 2: capture settebello
            withCapture.firstOrNull { it.capture!!.contains(settebello) }?.let { return it.card }
            // Priority 3: best primiera score
            return withCapture.maxByOrNull { it.score }!!.card
        }

        // No capture: discard keeping high-value cards
        // Also avoid leaving easy scopa for opponents if possible
        return hand.minByOrNull { card ->
            val base = primieraValue(card)
            val keepBonus = if (card.rank == Rank.SETTE) 50 else 0
            val denariBonus = if (card.suit == Suit.DENARI) 20 else 0
            base + keepBonus + denariBonus
        }!!
    }

    fun chooseCapture(state: ScoponeState, playerIndex: Int): List<Card> {
        val captures = state.pendingCaptures
        if (captures.isEmpty()) return emptyList()
        // Prefer capture containing settebello, else best primiera score
        return captures.maxByOrNull { group ->
            val settebelloBonus = if (group.contains(settebello)) 1000 else 0
            settebelloBonus + group.sumOf { primieraValue(it) }
        }!!
    }
}
