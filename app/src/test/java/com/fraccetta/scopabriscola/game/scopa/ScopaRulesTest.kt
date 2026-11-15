package com.fraccetta.scopabriscola.game.scopa

import com.fraccetta.scopabriscola.domain.*
import org.junit.Assert.*
import org.junit.Test

class ScopaRulesTest {

    @Test fun singleMatchForbidsMulti() {
        val played = Card(Rank.SETTE, Suit.DENARI)
        val table = listOf(Card(Rank.SETTE, Suit.COPPE), Card(Rank.TRE, Suit.BASTONI), Card(Rank.QUATTRO, Suit.SPADE))
        val captures = ScopaRules.validCaptures(played, table)
        assertEquals(1, captures.size)
        assertEquals(listOf(Card(Rank.SETTE, Suit.COPPE)), captures[0])
    }

    @Test fun multiCaptureWhenNoSingle() {
        val played = Card(Rank.SETTE, Suit.DENARI)
        val table = listOf(Card(Rank.TRE, Suit.BASTONI), Card(Rank.QUATTRO, Suit.SPADE))
        val captures = ScopaRules.validCaptures(played, table)
        assertEquals(1, captures.size)
        assertEquals(2, captures[0].size)
    }

    @Test fun scopaFalseOnLastCard() {
        // remaining = empty but it IS the last card of the round
        assertFalse(ScopaRules.isScopa(emptyList(), isLastCardOfRound = true))
    }

    @Test fun scopaTrueNormally() {
        // remaining = empty and NOT the last card
        assertTrue(ScopaRules.isScopa(emptyList(), isLastCardOfRound = false))
    }

    @Test fun scopaFalseWhenTableNotEmpty() {
        val remaining = listOf(Card(Rank.CINQUE, Suit.SPADE))
        assertFalse(ScopaRules.isScopa(remaining, isLastCardOfRound = false))
    }

    @Test fun noCapturePossible() {
        val played = Card(Rank.RE, Suit.SPADE)
        val table = listOf(Card(Rank.ASSO, Suit.DENARI), Card(Rank.DUE, Suit.COPPE))
        assertTrue(ScopaRules.validCaptures(played, table).isEmpty())
    }

    @Test fun multiCardClearanceIsScopa() {
        // play 7, table has [3, 4] — both captured, remaining empty → scopa
        assertTrue(ScopaRules.isScopa(emptyList(), isLastCardOfRound = false))
    }
}
