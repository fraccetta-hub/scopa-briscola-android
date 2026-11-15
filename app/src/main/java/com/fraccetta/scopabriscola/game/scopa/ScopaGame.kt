package com.fraccetta.scopabriscola.game.scopa

import com.fraccetta.scopabriscola.domain.Card
import com.fraccetta.scopabriscola.domain.Deck

data class ScopaState(
    val tableCards: List<Card>,
    val player1Hand: List<Card>,
    val player2Hand: List<Card>,
    val player1Captured: List<Card>,
    val player2Captured: List<Card>,
    val player1Scope: Int,
    val player2Scope: Int,
    val player1SessionScore: Int,
    val player2SessionScore: Int,
    val currentPlayer: Int,         // 1 or 2
    val phase: ScopaPhase,
    val pendingCaptures: List<List<Card>>,  // non-empty only in WAITING_CAPTURE_CHOICE
    val lastCapture: Pair<Int, List<Card>>?,  // (player, cards) for UI highlight
    val deckCards: List<Card>       // remaining deck for dealing mid-round
)

enum class ScopaPhase {
    WAITING_PLAY,
    WAITING_CAPTURE_CHOICE,
    ROUND_END,
    SESSION_END
}

sealed class ScopaAction {
    data class PlayCard(val card: Card) : ScopaAction()
    data class ChooseCapture(val captureGroup: List<Card>) : ScopaAction()
    object NextRound : ScopaAction()
}

class ScopaGame(val winScore: Int = 11) {

    fun newSession(): ScopaState {
        val deck = Deck.shuffled().toMutableList()
        val tableCards = deal(deck, 4)
        val p1Hand = deal(deck, 3)
        val p2Hand = deal(deck, 3)
        return ScopaState(
            tableCards = tableCards,
            player1Hand = p1Hand,
            player2Hand = p2Hand,
            player1Captured = emptyList(),
            player2Captured = emptyList(),
            player1Scope = 0,
            player2Scope = 0,
            player1SessionScore = 0,
            player2SessionScore = 0,
            currentPlayer = 1,
            phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(),
            lastCapture = null,
            deckCards = deck.toList()
        )
    }

    fun apply(state: ScopaState, action: ScopaAction): ScopaState {
        return when (action) {
            is ScopaAction.PlayCard -> handlePlayCard(state, action.card)
            is ScopaAction.ChooseCapture -> handleChooseCapture(state, action.captureGroup)
            is ScopaAction.NextRound -> handleNextRound(state)
        }
    }

    private fun deal(deck: MutableList<Card>, n: Int): List<Card> {
        val hand = deck.take(n)
        repeat(n) { if (deck.isNotEmpty()) deck.removeAt(0) }
        return hand
    }

    private fun handlePlayCard(state: ScopaState, card: Card): ScopaState {
        // Remove card from current player's hand
        val newP1Hand = if (state.currentPlayer == 1) state.player1Hand - card else state.player1Hand
        val newP2Hand = if (state.currentPlayer == 2) state.player2Hand - card else state.player2Hand

        val captures = ScopaRules.validCaptures(card, state.tableCards)

        return when {
            captures.isEmpty() -> {
                // No capture: add card to table, switch player
                state.copy(
                    tableCards = state.tableCards + card,
                    player1Hand = newP1Hand,
                    player2Hand = newP2Hand,
                    currentPlayer = if (state.currentPlayer == 1) 2 else 1,
                    lastCapture = null,
                    phase = ScopaPhase.WAITING_PLAY
                ).checkDealIfNeeded()
            }
            captures.size == 1 -> {
                // Single capture group: auto-capture
                val baseState = state.copy(
                    player1Hand = newP1Hand,
                    player2Hand = newP2Hand
                )
                executeCapture(baseState, card, captures[0])
            }
            else -> {
                // Multiple capture groups: wait for player choice
                state.copy(
                    player1Hand = newP1Hand,
                    player2Hand = newP2Hand,
                    phase = ScopaPhase.WAITING_CAPTURE_CHOICE,
                    pendingCaptures = captures
                )
            }
        }
    }

    private fun handleChooseCapture(state: ScopaState, captureGroup: List<Card>): ScopaState {
        // The card played is the one NOT in the hand and not on the table
        // We need to infer it from pendingCaptures context — but we don't store the played card.
        // Actually when WAITING_CAPTURE_CHOICE, the played card was removed from hand already.
        // We need to figure out the played card. The played card is the one that can capture the captureGroup.
        // Since multiple groups exist, let's find which card was played by looking at what's not in hand/table/captured.
        // The simplest approach: find any card that validCaptures would include captureGroup.
        // But we don't have the played card stored. Let's store it in pendingCaptures by convention or
        // reconstruct: the played card is a card whose rank sum or rank matches the captureGroup.
        //
        // Better: look at what card can capture the chosen group from the pending captures context.
        // Since validCaptures was called with the played card, and we have the captureGroup,
        // the played card's rank either matches a card in captureGroup (single-rank match)
        // or its numericValue equals sum of captureGroup values.
        //
        // Actually let's just figure the played card from: all cards that could produce pendingCaptures
        // when played against the CURRENT tableCards (before capture). The tableCards still contain the
        // captureGroup since we haven't removed it yet.
        //
        // Simplest: find any card not in hand, table, captured, or deck that has validCaptures on tableCards
        // that includes our captureGroup. This is complex.
        //
        // Better design: store played card in pendingCaptures or use a wrapper.
        // Since we can't change ScopaState signature without task approval, let's infer:
        // The played card has numericValue = sum of captureGroup IF no single-rank match;
        // OR has same rank as captureGroup[0] if it was a rank match (but rank match returns size==1 always).
        // Since we're here (size > 1), it must be a sum-capture. The played card's numericValue = sum of group.
        // But we don't know which played card. We need to find it.
        //
        // Actually the played card was removed from hand before calling handlePlayCard.
        // We can find all cards that:
        //   1) Are not in p1Hand, p2Hand, tableCards, p1Captured, p2Captured, deckCards
        //   2) validCaptures(card, tableCards) produces the same pendingCaptures
        //
        // Since a 40-card deck: played card = full deck minus all currently accounted-for cards
        val allCards = Deck.full().toSet()
        val accountedFor = (state.player1Hand + state.player2Hand + state.tableCards +
            state.player1Captured + state.player2Captured + state.deckCards).toSet()
        val playedCard = allCards.first { it !in accountedFor }

        val baseState = state.copy(phase = ScopaPhase.WAITING_PLAY, pendingCaptures = emptyList())
        return executeCapture(baseState, playedCard, captureGroup)
    }

    private fun handleNextRound(state: ScopaState): ScopaState {
        val deck = Deck.shuffled().toMutableList()
        val tableCards = deal(deck, 4)
        val p1Hand = deal(deck, 3)
        val p2Hand = deal(deck, 3)
        return state.copy(
            tableCards = tableCards,
            player1Hand = p1Hand,
            player2Hand = p2Hand,
            player1Captured = emptyList(),
            player2Captured = emptyList(),
            player1Scope = 0,
            player2Scope = 0,
            currentPlayer = 1,
            phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList(),
            lastCapture = null,
            deckCards = deck.toList()
        )
    }

    private fun executeCapture(state: ScopaState, playedCard: Card, captureGroup: List<Card>): ScopaState {
        val newTableCards = state.tableCards - captureGroup.toSet()
        val capturedCards = captureGroup + playedCard

        // Check if isLastCardOfRound: deck is empty AND both players will have empty hands AFTER this play
        val isLastCardOfRound = state.deckCards.isEmpty() &&
            state.player1Hand.isEmpty() &&
            state.player2Hand.isEmpty()

        val isScopa = ScopaRules.isScopa(newTableCards, isLastCardOfRound)

        val newP1Captured = if (state.currentPlayer == 1) state.player1Captured + capturedCards else state.player1Captured
        val newP2Captured = if (state.currentPlayer == 2) state.player2Captured + capturedCards else state.player2Captured
        val newP1Scope = if (state.currentPlayer == 1 && isScopa) state.player1Scope + 1 else state.player1Scope
        val newP2Scope = if (state.currentPlayer == 2 && isScopa) state.player2Scope + 1 else state.player2Scope
        val lastCapture = Pair(state.currentPlayer, capturedCards)
        val nextPlayer = if (state.currentPlayer == 1) 2 else 1

        val afterCapture = state.copy(
            tableCards = newTableCards,
            player1Captured = newP1Captured,
            player2Captured = newP2Captured,
            player1Scope = newP1Scope,
            player2Scope = newP2Scope,
            currentPlayer = nextPlayer,
            lastCapture = lastCapture,
            phase = ScopaPhase.WAITING_PLAY,
            pendingCaptures = emptyList()
        )

        // Check if both hands are empty
        return if (afterCapture.player1Hand.isEmpty() && afterCapture.player2Hand.isEmpty()) {
            if (afterCapture.deckCards.isNotEmpty()) {
                // Deal 3 more to each
                afterCapture.checkDealIfNeeded()
            } else {
                // End of round
                endRound(afterCapture, lastCapturer = state.currentPlayer)
            }
        } else {
            afterCapture
        }
    }

    private fun ScopaState.checkDealIfNeeded(): ScopaState {
        if (player1Hand.isEmpty() && player2Hand.isEmpty() && deckCards.isNotEmpty()) {
            val deck = deckCards.toMutableList()
            val p1Hand = deck.take(3)
            repeat(minOf(3, deck.size)) { deck.removeAt(0) }
            val p2Hand = deck.take(3)
            repeat(minOf(3, deck.size)) { deck.removeAt(0) }
            return copy(player1Hand = p1Hand, player2Hand = p2Hand, deckCards = deck.toList())
        }
        return this
    }

    private fun endRound(state: ScopaState, lastCapturer: Int): ScopaState {
        // Last player who captured gets remaining table cards (no scopa)
        val remainingTable = state.tableCards
        val finalP1Captured = if (lastCapturer == 1) state.player1Captured + remainingTable else state.player1Captured
        val finalP2Captured = if (lastCapturer == 2) state.player2Captured + remainingTable else state.player2Captured

        val (p1Score, p2Score) = ScopaScorer.scoreRound(
            finalP1Captured, state.player1Scope,
            finalP2Captured, state.player2Scope
        )

        val newP1Session = state.player1SessionScore + p1Score.total
        val newP2Session = state.player2SessionScore + p2Score.total

        val afterRoundState = state.copy(
            tableCards = emptyList(),
            player1Captured = finalP1Captured,
            player2Captured = finalP2Captured,
            player1SessionScore = newP1Session,
            player2SessionScore = newP2Session
        )

        // Check for session end
        val p1Wins = newP1Session >= winScore
        val p2Wins = newP2Session >= winScore

        return when {
            p1Wins && p2Wins -> {
                // Tie: whoever has more points wins; if still tied, another round
                when {
                    newP1Session > newP2Session -> afterRoundState.copy(phase = ScopaPhase.SESSION_END)
                    newP2Session > newP1Session -> afterRoundState.copy(phase = ScopaPhase.SESSION_END)
                    else -> afterRoundState.copy(phase = ScopaPhase.ROUND_END) // another round
                }
            }
            p1Wins || p2Wins -> afterRoundState.copy(phase = ScopaPhase.SESSION_END)
            else -> afterRoundState.copy(phase = ScopaPhase.ROUND_END)
        }
    }
}
