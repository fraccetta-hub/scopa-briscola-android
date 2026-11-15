package com.fraccetta.scopabriscola.domain

import org.junit.Assert.*
import org.junit.Test

class CardTest {
    @Test fun deckHas40Cards() = assertEquals(40, Deck.full().size)
    @Test fun deckAllUnique() = assertEquals(40, Deck.full().toSet().size)
    @Test fun assetNameFormat() = assertEquals("asso_denari", Card(Rank.ASSO, Suit.DENARI).assetName)
    @Test fun fanteAssetName() = assertEquals("fante_coppe", Card(Rank.FANTE, Suit.COPPE).assetName)
    @Test fun shuffledDeckSameCards() {
        val d = Deck.shuffled()
        assertEquals(40, d.size)
        assertEquals(Deck.full().toSet(), d.toSet())
    }
    @Test fun rankNumericValues() {
        assertEquals(1, Rank.ASSO.numericValue)
        assertEquals(7, Rank.SETTE.numericValue)
        assertEquals(10, Rank.RE.numericValue)
    }
    @Test fun deckOrderConsistent() {
        // Deck.full() must return cards in a consistent, documented order
        // First 10 cards are DENARI suit (Asso through Re)
        val full = Deck.full()
        assertEquals(Suit.DENARI, full[0].suit)
        assertEquals(Rank.ASSO, full[0].rank)
        assertEquals(Suit.DENARI, full[9].suit)
        assertEquals(Rank.RE, full[9].rank)
    }
}
