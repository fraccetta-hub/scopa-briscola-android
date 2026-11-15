package com.fraccetta.scopabriscola.game.briscola

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Rank
import com.fraccetta.scopabriscola.domain.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BriscolaGameTest {

    @Test fun dealGives3CardsEach() {
        val g = BriscolaGame()
        val s = g.newSession()
        assertEquals(3, s.player1Hand.size)
        assertEquals(3, s.player2Hand.size)
    }

    @Test fun lastCardIsBriscola() {
        val s = BriscolaGame().newSession()
        assertEquals(s.briscolaSuit, s.briscolaCard.suit)
    }

    @Test fun penultimateWinnerGetsBriscolaCard() {
        // Inject a state where deckCards has 1 card, p1 wins the trick
        // Setup: p1 plays briscola (wins), p2 plays non-briscola
        val briscolaCard = Card(Rank.ASSO, Suit.DENARI)
        val remainingDeckCard = Card(Rank.DUE, Suit.COPPE)

        val p1Card = Card(Rank.TRE, Suit.DENARI)  // briscola suit
        val p2Card = Card(Rank.RE, Suit.SPADE)     // non-briscola

        val state = BriscolaState(
            briscolaSuit = Suit.DENARI,
            briscolaCard = briscolaCard,
            deckCards = listOf(remainingDeckCard),
            player1Hand = listOf(p1Card),
            player2Hand = listOf(p2Card),
            player1Captured = emptyList(),
            player2Captured = emptyList(),
            player1SessionPoints = 0,
            player2SessionPoints = 0,
            currentPlayer = 1,
            firstCardPlayed = null,
            firstPlayerOfTrick = 1,
            phase = BriscolaPhase.WAITING_FIRST,
            briscolaCardTaken = false
        )

        val g = BriscolaGame()
        // p1 plays first
        val afterFirst = g.apply(state, p1Card)
        assertEquals(BriscolaPhase.WAITING_SECOND, afterFirst.phase)

        // p2 plays second
        val afterSecond = g.apply(afterFirst, p2Card)

        // p1 wins (briscola beats non-briscola), p1 should get: remainingDeckCard AND briscolaCard
        assertTrue(afterSecond.player1Hand.contains(remainingDeckCard))
        assertTrue(afterSecond.player1Hand.contains(briscolaCard))
        // p2 gets nothing (deck exhausted after winner takes)
        assertEquals(0, afterSecond.player2Hand.size)
    }

    @Test fun sessionEndsWhenCumulated121() {
        val g = BriscolaGame(winPoints = 121)

        // Build a state where the game is about to end:
        // p1 has one card (ASSO of briscola = 11 pts), p2 has one non-scoring card
        // deckCards is empty, briscolaCardTaken = true
        // p1SessionPoints = 110, so after game p1 would have 110 + (points from captured) >= 121
        val briscolaCard = Card(Rank.RE, Suit.DENARI)  // 4 pts, already taken
        val p1Card = Card(Rank.ASSO, Suit.DENARI)      // 11 pts, briscola
        val p2Card = Card(Rank.DUE, Suit.COPPE)        // 0 pts

        val state = BriscolaState(
            briscolaSuit = Suit.DENARI,
            briscolaCard = briscolaCard,
            deckCards = emptyList(),
            player1Hand = listOf(p1Card),
            player2Hand = listOf(p2Card),
            // p1 already captured cards worth 110 session pts worth of game points
            // We'll set sessionPoints directly and have captured pile with enough points
            player1Captured = listOf(Card(Rank.TRE, Suit.COPPE)), // 10 pts in captured
            player2Captured = emptyList(),
            player1SessionPoints = 100,  // already has 100; will add game points from captured (10 + 11 = 21) → 121
            player2SessionPoints = 0,
            currentPlayer = 1,
            firstCardPlayed = null,
            firstPlayerOfTrick = 1,
            phase = BriscolaPhase.WAITING_FIRST,
            briscolaCardTaken = true
        )

        // p1 plays briscola asso (wins), p2 plays non-scoring card
        val afterFirst = g.apply(state, p1Card)
        val afterSecond = g.apply(afterFirst, p2Card)

        // Both hands empty, game ends, p1 session pts = 100 + (10+11+0) = 121 >= 121 → SESSION_END
        assertEquals(BriscolaPhase.SESSION_END, afterSecond.phase)
        assertEquals(121, afterSecond.player1SessionPoints)
    }
}
