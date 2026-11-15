package com.fraccetta.scopabriscola.game.scopa

import com.fraccetta.scopabriscola.domain.*
import org.junit.Assert.*
import org.junit.Test

class ScopaScorerTest {

    @Test fun settebelloGoesToOwner() {
        val settebello = Card(Rank.SETTE, Suit.DENARI)
        val p1 = listOf(settebello)
        val (s1, s2) = ScopaScorer.scoreRound(p1, 0, emptyList(), 0)
        assertEquals(1, s1.settebello)
        assertEquals(0, s2.settebello)
    }

    @Test fun carteParity() {
        val half = Deck.full().take(20)
        val other = Deck.full().drop(20)
        val (s1, s2) = ScopaScorer.scoreRound(half, 0, other, 0)
        assertEquals(0, s1.carte + s2.carte)  // parity → nobody
    }

    @Test fun carteMajority() {
        val p1 = Deck.full().take(21)
        val p2 = Deck.full().drop(21)
        val (s1, _) = ScopaScorer.scoreRound(p1, 0, p2, 0)
        assertEquals(1, s1.carte)
    }

    @Test fun primieraWinner() {
        // p1 has 7 of every suit (best primiera), p2 has the rest
        val p1 = Suit.entries.map { Card(Rank.SETTE, it) }
        val p2 = Deck.full().filter { it !in p1 }
        val (s1, s2) = ScopaScorer.scoreRound(p1, 0, p2, 0)
        assertEquals(1, s1.primiera)
        assertEquals(0, s2.primiera)
    }

    @Test fun primieraWithMissingSuit() {
        // p1 has no BASTONI cards → that suit contributes 0 to p1's primiera
        val p1 = Deck.full().filter { it.suit != Suit.BASTONI }.take(10)
        val p2 = Deck.full().filter { it !in p1 }
        val (s1, s2) = ScopaScorer.scoreRound(p1, 0, p2, 0)
        // p2 has BASTONI so likely wins primiera (not asserting winner, just no crash)
        assertEquals(1, s1.primiera + s2.primiera)
    }

    @Test fun scopeCountedInTotal() {
        val p1 = Deck.full().take(20)
        val (s1, _) = ScopaScorer.scoreRound(p1, 3, Deck.full().drop(20), 0)
        assertEquals(3, s1.scope)
    }

    @Test fun primieraValues() {
        assertEquals(21, ScopaScorer.primieraValue(Rank.SETTE))
        assertEquals(18, ScopaScorer.primieraValue(Rank.SEI))
        assertEquals(16, ScopaScorer.primieraValue(Rank.ASSO))
        assertEquals(10, ScopaScorer.primieraValue(Rank.RE))
        assertEquals(10, ScopaScorer.primieraValue(Rank.FANTE))
    }
}
