package com.fraccetta.scopabriscola.domain

data class SessionResult(
    val winnerPlayer: Int,  // 1 or 2
    val player1FinalScore: Int,
    val player2FinalScore: Int,
    val gameType: String    // "SCOPA"
)
