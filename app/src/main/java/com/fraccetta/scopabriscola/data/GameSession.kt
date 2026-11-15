package com.fraccetta.scopabriscola.data

/**
 * In-memory singleton that carries the server/client role from the lobby to the game screen.
 * This is set by LobbyViewModel before navigating to the game.
 */
object GameSession {
    var isServer: Boolean = false
    var opponentName: String? = null
}
