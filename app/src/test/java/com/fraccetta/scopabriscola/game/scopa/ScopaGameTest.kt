package com.fraccetta.scopabriscola.game.scopa

import com.fraccetta.scopabriscola.domain.*
import org.junit.Assert.*
import org.junit.Test

class ScopaGameTest {

    private val game = ScopaGame(winScore = 11)

    @Test fun newSessionDeal() {
        val s = game.newSession()
        assertEquals(3, s.player1Hand.size)
        assertEquals(3, s.player2Hand.size)
        assertEquals(4, s.tableCards.size)
        assertEquals(1, s.currentPlayer)
        assertEquals(ScopaPhase.WAITING_PLAY, s.phase)
        assertEquals(0, s.player1SessionScore)
        assertEquals(0, s.player2SessionScore)
    }

    @Test fun playCardNoCapture_goesToTable() {
        val g = ScopaGame(winScore = 11)
        var s = g.newSession()
        // Find a card in p1's hand that cannot capture anything
        val noCapCard = s.player1Hand.firstOrNull { card ->
            ScopaRules.validCaptures(card, s.tableCards).isEmpty()
        } ?: return  // skip if none available
        val prevTableSize = s.tableCards.size
        s = g.apply(s, ScopaAction.PlayCard(noCapCard))
        assertEquals(prevTableSize + 1, s.tableCards.size)
        assertEquals(2, s.currentPlayer)
    }

    @Test fun playCardSingleCapture_takesCards() {
        // Craft a deterministic state with a guaranteed single capture
        val card = Card(Rank.ASSO, Suit.DENARI)
        val tableCard = Card(Rank.ASSO, Suit.COPPE)
        val state = ScopaState(
            tableCards = listOf(tableCard, Card(Rank.DUE, Suit.SPADE)),
            player1Hand = listOf(card, Card(Rank.TRE, Suit.BASTONI), Card(Rank.QUATTRO, Suit.COPPE)),
            player2Hand = listOf(Card(Rank.RE, Suit.SPADE), Card(Rank.CAVALLO, Suit.COPPE), Card(Rank.FANTE, Suit.BASTONI)),
            player1Captured = emptyList(), player2Captured = emptyList(),
            player1Scope = 0, player2Scope = 0,
            player1SessionScore = 0, player2SessionScore = 0,
            currentPlayer = 1, phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(), lastCapture = null,
            deckCards = Deck.full().filter { it !in listOf(
                card, tableCard,
                Card(Rank.DUE, Suit.SPADE),
                Card(Rank.TRE, Suit.BASTONI), Card(Rank.QUATTRO, Suit.COPPE),
                Card(Rank.RE, Suit.SPADE), Card(Rank.CAVALLO, Suit.COPPE), Card(Rank.FANTE, Suit.BASTONI)
            )}
        )
        val g = ScopaGame()
        val next = g.apply(state, ScopaAction.PlayCard(card))
        // p1 captured the asso
        assertTrue(next.player1Captured.contains(card))
        assertTrue(next.player1Captured.contains(tableCard))
        // current player switched to 2
        assertEquals(2, next.currentPlayer)
    }

    @Test fun multiCapture_setsWaitingCaptureChoice() {
        // 7 played against [asso(1)+sei(6), tre(3)+quattro(4)] — two distinct capture groups
        val played = Card(Rank.SETTE, Suit.DENARI)
        val asso = Card(Rank.ASSO, Suit.BASTONI)
        val sei = Card(Rank.SEI, Suit.SPADE)
        val tre = Card(Rank.TRE, Suit.BASTONI)
        val quattro = Card(Rank.QUATTRO, Suit.SPADE)
        val state = ScopaState(
            tableCards = listOf(asso, sei, tre, quattro),
            player1Hand = listOf(played, Card(Rank.DUE, Suit.COPPE), Card(Rank.CINQUE, Suit.SPADE)),
            player2Hand = listOf(Card(Rank.RE, Suit.DENARI), Card(Rank.CAVALLO, Suit.COPPE), Card(Rank.FANTE, Suit.BASTONI)),
            player1Captured = emptyList(), player2Captured = emptyList(),
            player1Scope = 0, player2Scope = 0,
            player1SessionScore = 0, player2SessionScore = 0,
            currentPlayer = 1, phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(), lastCapture = null,
            deckCards = Deck.full().filter { it !in listOf(
                played, asso, sei, tre, quattro,
                Card(Rank.DUE, Suit.COPPE), Card(Rank.CINQUE, Suit.SPADE),
                Card(Rank.RE, Suit.DENARI), Card(Rank.CAVALLO, Suit.COPPE), Card(Rank.FANTE, Suit.BASTONI)
            )}
        )
        val g = ScopaGame()
        val next = g.apply(state, ScopaAction.PlayCard(played))
        // [1+6] and [3+4] are two valid groups → size==2 → WAITING_CAPTURE_CHOICE
        assertEquals(ScopaPhase.WAITING_CAPTURE_CHOICE, next.phase)
        assertEquals(2, next.pendingCaptures.size)
        assertEquals(1, next.currentPlayer) // player not switched yet

        // Apply ChooseCapture and verify capture completes
        val afterCapture = g.apply(next, ScopaAction.ChooseCapture(listOf(asso, sei)))
        assertTrue(afterCapture.player1Captured.containsAll(listOf(played, asso, sei)))
        assertEquals(2, afterCapture.currentPlayer)
        assertEquals(ScopaPhase.WAITING_PLAY, afterCapture.phase)
    }

    @Test fun sessionEnds_whenScoreReachesWin() {
        // Inject final-card scenario: p1 plays last card capturing everything → endRound → SESSION_END
        // p1 already has majority of denari (5) and settebello captured; p2 has little.
        // With winScore=1, any point ends the session.
        val g = ScopaGame(winScore = 1)

        // p1 plays ASSO_DENARI, table has ASSO_COPPE (single rank match → auto-capture → scopa if table clears)
        // After capture: table empty, both hands empty, deck empty → endRound → p1 scores scope → SESSION_END
        val played = Card(Rank.ASSO, Suit.DENARI)
        val tableCard = Card(Rank.ASSO, Suit.COPPE)

        // Build a full 40-card accounting: played + tableCard + p2Hand + p1Captured + p2Captured = 40
        val p2Hand = listOf(Card(Rank.RE, Suit.SPADE), Card(Rank.CAVALLO, Suit.COPPE), Card(Rank.FANTE, Suit.BASTONI))
        val p1Captured = listOf(
            Card(Rank.SETTE, Suit.DENARI), // settebello — gives p1 1 point for settebello alone
            Card(Rank.DUE, Suit.DENARI), Card(Rank.TRE, Suit.DENARI), Card(Rank.QUATTRO, Suit.DENARI),
            Card(Rank.CINQUE, Suit.DENARI)
        )
        val p2Captured = listOf(
            Card(Rank.DUE, Suit.COPPE), Card(Rank.TRE, Suit.COPPE), Card(Rank.QUATTRO, Suit.COPPE),
            Card(Rank.CINQUE, Suit.COPPE), Card(Rank.SEI, Suit.COPPE), Card(Rank.FANTE, Suit.COPPE),
            Card(Rank.CAVALLO, Suit.DENARI), Card(Rank.RE, Suit.DENARI)
        )
        val p1Hand = listOf(played)
        val remainingDeck = Deck.full().filter { it !in
            (p1Hand + listOf(tableCard) + p2Hand + p1Captured + p2Captured)
        }

        val state = ScopaState(
            tableCards = listOf(tableCard),
            player1Hand = p1Hand,
            player2Hand = p2Hand,
            player1Captured = p1Captured,
            player2Captured = p2Captured,
            player1Scope = 0, player2Scope = 0,
            player1SessionScore = 0, player2SessionScore = 0,
            currentPlayer = 1, phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(), lastCapture = null,
            deckCards = remainingDeck
        )

        // p1 plays ASSO_DENARI → captures ASSO_COPPE (single rank match, auto) → table clears
        // isLastCardOfRound = deckCards.isEmpty() && p1Hand.isEmpty() && p2Hand.isEmpty()
        // deckCards is NOT empty here (remainingDeck has cards), so NOT last card of round
        // After capture: p1Hand empty, p2Hand has 3, deck has cards → deal more, game continues
        // This path won't immediately SESSION_END. Instead, test endRound directly via a simpler state.

        // Simpler: inject state where deck empty, p2Hand empty, p1 has last card capturing last table card
        val lastPlayed = Card(Rank.SEI, Suit.SPADE)
        val lastTable = Card(Rank.SEI, Suit.BASTONI)
        val allOtherCards = Deck.full().filter { it != lastPlayed && it != lastTable }
        // Split all other 38 cards between captured piles
        val bigP1Captured = allOtherCards.take(19)
        val bigP2Captured = allOtherCards.drop(19)

        val finalState = ScopaState(
            tableCards = listOf(lastTable),
            player1Hand = listOf(lastPlayed),
            player2Hand = emptyList(),
            player1Captured = bigP1Captured,
            player2Captured = bigP2Captured,
            player1Scope = 0, player2Scope = 0,
            player1SessionScore = 0, player2SessionScore = 0,
            currentPlayer = 1, phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(), lastCapture = null,
            deckCards = emptyList()
        )
        val result = g.apply(finalState, ScopaAction.PlayCard(lastPlayed))
        // isLastCardOfRound = deckEmpty && p1Hand(after play)=empty && p2Hand=empty → true → no scopa
        // endRound called → scoring → SESSION_END (winScore=1, someone will score ≥ 1)
        assertEquals(ScopaPhase.SESSION_END, result.phase)
        assertTrue(result.player1SessionScore + result.player2SessionScore >= 1)
    }

    @Test fun deckCardsDecreaseAfterDeal() {
        val s = game.newSession()
        // Initial deck: 40 - 4 (table) - 3 (p1) - 3 (p2) = 30 remaining
        assertEquals(30, s.deckCards.size)
    }

    @Test fun totalCardsConstant() {
        val s = game.newSession()
        val total = s.tableCards.size + s.player1Hand.size + s.player2Hand.size +
            s.player1Captured.size + s.player2Captured.size + s.deckCards.size
        assertEquals(40, total)
    }
}
