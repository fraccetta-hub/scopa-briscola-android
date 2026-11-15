package com.fraccetta.scopabriscola.game.briscola

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Rank
import com.fraccetta.scopabriscola.domain.Suit
import org.junit.Assert.assertEquals
import org.junit.Test

class BriscolaRulesTest {

    @Test fun cardPoints() {
        assertEquals(11, BriscolaRules.cardPoints(Rank.ASSO))
        assertEquals(10, BriscolaRules.cardPoints(Rank.TRE))
        assertEquals(4, BriscolaRules.cardPoints(Rank.RE))
        assertEquals(3, BriscolaRules.cardPoints(Rank.CAVALLO))
        assertEquals(2, BriscolaRules.cardPoints(Rank.FANTE))
        assertEquals(0, BriscolaRules.cardPoints(Rank.SETTE))
    }

    @Test fun briscolaBeatsHigherCard() {
        val briscolaCard = Card(Rank.DUE, Suit.DENARI)  // briscola bassa
        val highCard = Card(Rank.ASSO, Suit.COPPE)        // asso non-briscola
        // p2 risponde con briscola bassa, p1 ha giocato asso: vince p2
        assertEquals(2, BriscolaRules.trickWinner(highCard, briscolaCard, 1, Suit.DENARI))
    }

    @Test fun higherCardSameSuitWins() {
        val first = Card(Rank.RE, Suit.SPADE)
        val second = Card(Rank.ASSO, Suit.SPADE)
        // p2 risponde con asso stesso seme: vince p2
        assertEquals(2, BriscolaRules.trickWinner(first, second, 1, Suit.DENARI))
    }

    @Test fun differentSuiteNoBriscolaFirstWins() {
        val first = Card(Rank.DUE, Suit.COPPE)
        val second = Card(Rank.RE, Suit.SPADE)   // re seme diverso, no briscola
        assertEquals(1, BriscolaRules.trickWinner(first, second, 1, Suit.DENARI))
    }
}
