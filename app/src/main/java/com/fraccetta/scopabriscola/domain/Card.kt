package com.fraccetta.scopabriscola.domain

enum class Suit { DENARI, COPPE, SPADE, BASTONI }

enum class Rank(val displayName: String, val numericValue: Int) {
    ASSO("asso", 1),
    DUE("2", 2),
    TRE("3", 3),
    QUATTRO("4", 4),
    CINQUE("5", 5),
    SEI("6", 6),
    SETTE("7", 7),
    FANTE("fante", 8),
    CAVALLO("cavallo", 9),
    RE("re", 10)
}

data class Card(val rank: Rank, val suit: Suit) {
    val assetName: String get() = "${rank.displayName}_${suit.name.lowercase()}"
}

object Deck {
    /** Returns 40 cards ordered by suit (DENARI, COPPE, SPADE, BASTONI), then by rank (ASSO..RE). */
    fun full(): List<Card> = Suit.values().flatMap { suit ->
        Rank.values().map { rank -> Card(rank, suit) }
    }

    fun shuffled(): List<Card> = full().shuffled()
}
